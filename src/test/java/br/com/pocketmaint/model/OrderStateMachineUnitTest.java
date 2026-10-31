package br.com.pocketmaint.model;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.pocketmaint.exception.BusinessValidationException;
import br.com.pocketmaint.exception.InvalidOrderStateTransitionException;
import br.com.pocketmaint.exception.OrderAccessDeniedException;
import br.com.pocketmaint.model.enums.OrderStatus;
import br.com.pocketmaint.model.enums.Priority;
import br.com.pocketmaint.model.enums.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateMachineUnitTest {

    private User supervisor;
    private User technicianCarlos;
    private User technicianAna;
    private User inactiveTech;
    private Equipment equipment;
    private Sector sector;
    private ServiceOrder order;

    @BeforeEach
    void setUp() {
        supervisor = User.builder()
                .id(1L)
                .username("sup.roberto")
                .fullName("Roberto Lima")
                .role(Role.SUPERVISOR)
                .active(true)
                .build();

        technicianCarlos = User.builder()
                .id(2L)
                .username("tec.carlos")
                .fullName("Carlos Mendes")
                .role(Role.TECHNICIAN)
                .active(true)
                .build();

        technicianAna = User.builder()
                .id(3L)
                .username("tec.ana")
                .fullName("Ana Paula")
                .role(Role.TECHNICIAN)
                .active(true)
                .build();

        inactiveTech = User.builder()
                .id(4L)
                .username("tec.inativo")
                .fullName("Tecnico Inativo")
                .role(Role.TECHNICIAN)
                .active(false)
                .build();

        sector = Sector.builder()
                .id(1L)
                .code("USIN-01")
                .name("Setor de Usinagem")
                .active(true)
                .build();

        equipment = Equipment.builder()
                .id(1L)
                .assetTag("TAG-CNC-001")
                .name("Torno CNC Romi")
                .sector(sector)
                .active(true)
                .build();

        order = ServiceOrder.builder()
                .id(100L)
                .orderNumber("OS-2026-0001")
                .equipment(equipment)
                .sector(sector)
                .createdBy(supervisor)
                .title("Falha no fuso principal")
                .problemDescription("Ruído excessivo e vibração ao ultrapassar 2000 RPM.")
                .priority(Priority.HIGH)
                .status(OrderStatus.OPEN)
                .createdAt(Instant.now())
                .version(0L)
                .build();
    }

    @Nested
    @DisplayName("1. Fluxo Feliz de Ciclo de Vida Completo")
    class HappyPathTests {

        @Test
        @DisplayName("Deve transicionar com sucesso: OPEN -> ASSIGNED -> IN_PROGRESS -> COMPLETED")
        void shouldTransitionThroughFullLifecycleSuccessfully() {
            // 1. Estado Inicial: OPEN
            assertEquals(OrderStatus.OPEN, order.getStatus());
            assertNull(order.getAssignedTechnician());
            assertNull(order.getStartedAt());
            assertNull(order.getCompletedAt());

            // 2. T-02: OPEN -> ASSIGNED (BR-002)
            boolean assigned = order.assign(technicianCarlos, supervisor, "Atribuído no turno matutino");
            assertTrue(assigned);
            assertEquals(OrderStatus.ASSIGNED, order.getStatus());
            assertEquals(technicianCarlos, order.getAssignedTechnician());
            assertNotNull(order.getAssignedAt());

            // 3. T-03: ASSIGNED -> IN_PROGRESS (BR-003)
            boolean started = order.startAttendance(technicianCarlos.getId());
            assertTrue(started);
            assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
            assertNotNull(order.getStartedAt());

            // 4. T-04: IN_PROGRESS -> COMPLETED (BR-003, BR-004, BR-005)
            String solution = "Substituído rolamento do fuso e rebalanceado conjunto.";
            boolean completed = order.complete(technicianCarlos.getId(), solution);
            assertTrue(completed);
            assertEquals(OrderStatus.COMPLETED, order.getStatus());
            assertEquals(solution, order.getSolutionDescription());
            assertNotNull(order.getCompletedAt());
            assertTrue(order.isTerminal());
        }
    }

    @Nested
    @DisplayName("2. Transições Proibidas da Máquina de Estados")
    class ForbiddenTransitionTests {

        @Test
        @DisplayName("BR-004: Proibido pular direto de ASSIGNED para COMPLETED sem iniciar atendimento")
        void shouldNotAllowCompletionDirectlyFromAssigned() {
            order.assign(technicianCarlos, supervisor, "Atribuição");
            assertEquals(OrderStatus.ASSIGNED, order.getStatus());

            InvalidOrderStateTransitionException ex = assertThrows(
                    InvalidOrderStateTransitionException.class,
                    () -> order.complete(technicianCarlos.getId(), "Solução aplicada diretamente.")
            );

            assertTrue(ex.getMessage().contains("IN_PROGRESS"));
            assertEquals(OrderStatus.ASSIGNED, order.getStatus());
        }

        @Test
        @DisplayName("BR-006: Proibido cancelar ordem que já se encontra concluída (COMPLETED)")
        void shouldNotAllowCancellingCompletedOrder() {
            order.assign(technicianCarlos, supervisor, "Atribuição");
            order.startAttendance(technicianCarlos.getId());
            order.complete(technicianCarlos.getId(), "Troca do fuso e rolamentos.");
            assertEquals(OrderStatus.COMPLETED, order.getStatus());

            InvalidOrderStateTransitionException ex = assertThrows(
                    InvalidOrderStateTransitionException.class,
                    () -> order.cancel(supervisor, "Tentativa de cancelamento pós-conclusão")
            );

            assertTrue(ex.getMessage().contains("concluída"));
            assertEquals(OrderStatus.COMPLETED, order.getStatus());
        }

        @Test
        @DisplayName("Proibido reatribuir ordem que já está em andamento (IN_PROGRESS)")
        void shouldNotAllowReassigningOrderInProgress() {
            order.assign(technicianCarlos, supervisor, "Atribuição inicial");
            order.startAttendance(technicianCarlos.getId());
            assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());

            InvalidOrderStateTransitionException ex = assertThrows(
                    InvalidOrderStateTransitionException.class,
                    () -> order.assign(technicianAna, supervisor, "Tentando trocar técnico em atendimento")
            );

            assertTrue(ex.getMessage().contains("IN_PROGRESS"));
            assertEquals(technicianCarlos, order.getAssignedTechnician());
        }

        @Test
        @DisplayName("Proibido atribuir ordem que está em estado terminal (CANCELLED)")
        void shouldNotAllowAssigningCancelledOrder() {
            order.cancel(supervisor, "Equipamento transferido para descarte.");
            assertEquals(OrderStatus.CANCELLED, order.getStatus());

            InvalidOrderStateTransitionException ex = assertThrows(
                    InvalidOrderStateTransitionException.class,
                    () -> order.assign(technicianCarlos, supervisor, "Tentativa em OS cancelada")
            );

            assertTrue(ex.getMessage().contains("terminal"));
        }

        @Test
        @DisplayName("Proibido concluir ordem cancelada")
        void shouldNotAllowCompletingCancelledOrder() {
            order.assign(technicianCarlos, supervisor, "Atribuição");
            order.cancel(supervisor, "Cancelado por falta de peças sobressalentes.");
            assertEquals(OrderStatus.CANCELLED, order.getStatus());

            InvalidOrderStateTransitionException ex = assertThrows(
                    InvalidOrderStateTransitionException.class,
                    () -> order.complete(technicianCarlos.getId(), "Solução em OS cancelada")
            );

            assertTrue(ex.getMessage().contains("cancelada"));
        }
    }

    @Nested
    @DisplayName("3. Regras de Validação de Borda e Negócio")
    class ValidationRulesTests {

        @Test
        @DisplayName("BR-005: Proibido concluir com descrição de solução menor que 10 caracteres")
        void shouldRejectSolutionShorterThan10Chars() {
            order.assign(technicianCarlos, supervisor, "Atribuição");
            order.startAttendance(technicianCarlos.getId());

            BusinessValidationException ex = assertThrows(
                    BusinessValidationException.class,
                    () -> order.complete(technicianCarlos.getId(), "Feito.")
            );

            assertTrue(ex.getMessage().contains("10 e 500"));
            assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
        }

        @Test
        @DisplayName("BR-005: Proibido concluir com descrição de solução nula ou maior que 500 caracteres")
        void shouldRejectSolutionNullOrLongerThan500Chars() {
            order.assign(technicianCarlos, supervisor, "Atribuição");
            order.startAttendance(technicianCarlos.getId());

            assertThrows(
                    BusinessValidationException.class,
                    () -> order.complete(technicianCarlos.getId(), null)
            );

            String solution501 = "a".repeat(501);
            assertThrows(
                    BusinessValidationException.class,
                    () -> order.complete(technicianCarlos.getId(), solution501)
            );
        }

        @Test
        @DisplayName("BR-006: Proibido cancelar com justificativa menor que 10 ou maior que 300 caracteres")
        void shouldRejectCancellationReasonOutOfBounds() {
            // Menor que 10 caracteres
            BusinessValidationException exShort = assertThrows(
                    BusinessValidationException.class,
                    () -> order.cancel(supervisor, "Motivo")
            );
            assertTrue(exShort.getMessage().contains("10 e 300"));

            // Nulo
            assertThrows(
                    BusinessValidationException.class,
                    () -> order.cancel(supervisor, null)
            );

            // Maior que 300 caracteres
            String reason301 = "a".repeat(301);
            BusinessValidationException exLong = assertThrows(
                    BusinessValidationException.class,
                    () -> order.cancel(supervisor, reason301)
            );
            assertTrue(exLong.getMessage().contains("10 e 300"));
        }

        @Test
        @DisplayName("BR-002: Proibido atribuir ordem para técnico inativo ou perfil diferente de TECHNICIAN")
        void shouldRejectAssignmentToInactiveOrNonTechnician() {
            // Técnico inativo
            BusinessValidationException exInactive = assertThrows(
                    BusinessValidationException.class,
                    () -> order.assign(inactiveTech, supervisor, "Atribuição para inativo")
            );
            assertTrue(exInactive.getMessage().contains("ativo"));

            // Usuário com perfil SUPERVISOR não pode ser técnico da ordem
            BusinessValidationException exSupervisor = assertThrows(
                    BusinessValidationException.class,
                    () -> order.assign(supervisor, supervisor, "Atribuição para supervisor")
            );
            assertTrue(exSupervisor.getMessage().contains("TECHNICIAN"));
        }

        @Test
        @DisplayName("BR-003 Anti-IDOR: Técnico não designado não pode iniciar ou concluir atendimento")
        void shouldDenyAccessWhenOtherTechnicianAttemptsAction() {
            order.assign(technicianCarlos, supervisor, "Designado para Carlos");

            // Ana tenta iniciar ordem do Carlos
            OrderAccessDeniedException exStart = assertThrows(
                    OrderAccessDeniedException.class,
                    () -> order.startAttendance(technicianAna.getId())
            );
            assertTrue(exStart.getMessage().contains("técnico responsável"));

            // Carlos inicia normalmente
            order.startAttendance(technicianCarlos.getId());

            // Ana tenta concluir ordem do Carlos
            OrderAccessDeniedException exComplete = assertThrows(
                    OrderAccessDeniedException.class,
                    () -> order.complete(technicianAna.getId(), "Solução aplicada pela Ana.")
            );
            assertTrue(exComplete.getMessage().contains("técnico responsável"));
        }
    }

    @Nested
    @DisplayName("4. Comportamento Idempotente (BR-007)")
    class IdempotencyTests {

        @Test
        @DisplayName("Chamar startAttendance duas vezes para o mesmo técnico deve ser idempotente (retornar false)")
        void shouldBeIdempotentOnStartAttendanceRepeat() {
            order.assign(technicianCarlos, supervisor, "Atribuição");

            // Primeira chamada: transiciona
            boolean firstCall = order.startAttendance(technicianCarlos.getId());
            assertTrue(firstCall);
            assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
            Instant firstStartedAt = order.getStartedAt();
            assertNotNull(firstStartedAt);

            // Segunda chamada: idempotente (sem erro, devolve false, mantém startedAt)
            boolean secondCall = order.startAttendance(technicianCarlos.getId());
            assertFalse(secondCall);
            assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
            assertEquals(firstStartedAt, order.getStartedAt());
        }

        @Test
        @DisplayName("Chamar assign para o mesmo técnico já atribuído deve ser idempotente (retornar false)")
        void shouldBeIdempotentOnAssignRepeat() {
            boolean firstAssign = order.assign(technicianCarlos, supervisor, "Primeira atribuição");
            assertTrue(firstAssign);

            boolean secondAssign = order.assign(technicianCarlos, supervisor, "Mesma atribuição");
            assertFalse(secondAssign);
            assertEquals(OrderStatus.ASSIGNED, order.getStatus());
            assertEquals(technicianCarlos, order.getAssignedTechnician());
        }

        @Test
        @DisplayName("Chamar cancel em ordem já cancelada deve ser idempotente (retornar false)")
        void shouldBeIdempotentOnCancelRepeat() {
            boolean firstCancel = order.cancel(supervisor, "Primeiro cancelamento válido");
            assertTrue(firstCancel);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());

            boolean secondCancel = order.cancel(supervisor, "Segundo cancelamento válido");
            assertFalse(secondCancel);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }

        @Test
        @DisplayName("Chamar complete com a mesma solução já gravada deve ser idempotente (retornar false)")
        void shouldBeIdempotentOnCompleteRepeatWithSameSolution() {
            order.assign(technicianCarlos, supervisor, "Atribuição");
            order.startAttendance(technicianCarlos.getId());

            String solution = "Solução idêntica para o atendimento.";
            boolean firstComplete = order.complete(technicianCarlos.getId(), solution);
            assertTrue(firstComplete);

            boolean secondComplete = order.complete(technicianCarlos.getId(), solution);
            assertFalse(secondComplete);
            assertEquals(OrderStatus.COMPLETED, order.getStatus());
        }
    }
}
