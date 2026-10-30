package br.com.aberturaordensservico.model;

import java.time.Instant;

import br.com.aberturaordensservico.exception.BusinessValidationException;
import br.com.aberturaordensservico.exception.InvalidOrderStateTransitionException;
import br.com.aberturaordensservico.exception.OrderAccessDeniedException;
import br.com.aberturaordensservico.model.enums.OrderStatus;
import br.com.aberturaordensservico.model.enums.Priority;
import br.com.aberturaordensservico.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_orders")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(name = "problem_description", nullable = false, length = 500)
    private String problemDescription;

    @Column(name = "solution_description", length = 500)
    private String solutionDescription;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.OPEN;
        }
    }

    // =========================================================================
    // Métodos de Negócio e Máquina de Estados (BR-001 a BR-008)
    // =========================================================================

    /**
     * T-02: Atribuir ordem a um técnico (BR-002, BR-006)
     * @return true se o estado foi modificado; false se requisição foi idempotente
     */
    public boolean assign(User technician, User assignedBy, String notes) {
        if (isTerminal()) {
            throw new InvalidOrderStateTransitionException(
                    "Não é possível atribuir uma ordem em estado terminal: " + this.status
            );
        }
        if (this.status == OrderStatus.IN_PROGRESS) {
            throw new InvalidOrderStateTransitionException(
                    "A ordem já está em andamento (IN_PROGRESS) e não pode ser reatribuída diretamente."
            );
        }
        if (technician == null || !technician.isActive() || technician.getRole() != Role.TECHNICIAN) {
            throw new BusinessValidationException("A ordem só pode ser atribuída a um usuário ativo com perfil TECHNICIAN.");
        }

        // Idempotência (BR-007): se já está atribuída ao mesmo técnico
        if (this.assignedTechnician != null && this.assignedTechnician.getId().equals(technician.getId())
                && this.status == OrderStatus.ASSIGNED) {
            return false;
        }

        this.assignedTechnician = technician;
        this.status = OrderStatus.ASSIGNED;
        this.assignedAt = Instant.now();
        return true;
    }

    /**
     * T-03: Iniciar Atendimento (BR-003, BR-007)
     * @return true se transicionou; false se idempotente
     */
    public boolean startAttendance(Long technicianId) {
        validateTechnicianOwnership(technicianId, "iniciar o atendimento");

        // Idempotência (BR-007): já está em IN_PROGRESS para o mesmo técnico
        if (this.status == OrderStatus.IN_PROGRESS) {
            return false;
        }

        if (this.status != OrderStatus.ASSIGNED) {
            throw new InvalidOrderStateTransitionException(
                    "Apenas ordens no estado ASSIGNED podem ser iniciadas. Estado atual: " + this.status
            );
        }

        this.status = OrderStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        return true;
    }

    /**
     * T-04: Concluir Ordem com Solução (BR-003, BR-004, BR-005, BR-006, BR-007)
     * @return true se transicionou; false se idempotente
     */
    public boolean complete(Long technicianId, String solution) {
        validateTechnicianOwnership(technicianId, "concluir");

        if (this.status == OrderStatus.COMPLETED) {
            // Idempotência (BR-007): já concluída
            if (this.solutionDescription != null && this.solutionDescription.equals(solution != null ? solution.trim() : "")) {
                return false;
            }
            throw new InvalidOrderStateTransitionException("Esta ordem de serviço já se encontra concluída.");
        }

        if (this.status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateTransitionException("Não é possível concluir uma ordem cancelada.");
        }

        // BR-004: Proibição de conclusão direta sem passar por IN_PROGRESS
        if (this.status != OrderStatus.IN_PROGRESS) {
            throw new InvalidOrderStateTransitionException(
                    "A ordem de serviço precisa ser iniciada (IN_PROGRESS) antes de ser concluída. Estado atual: " + this.status
            );
        }

        // BR-005: Obrigatoriedade da descrição da solução (10 a 500 caracteres)
        if (solution == null || solution.trim().length() < 10 || solution.trim().length() > 500) {
            throw new BusinessValidationException("A descrição da solução deve ter entre 10 e 500 caracteres.");
        }

        this.status = OrderStatus.COMPLETED;
        this.solutionDescription = solution.trim();
        this.completedAt = Instant.now();
        return true;
    }

    /**
     * T-05: Cancelar Ordem de Serviço (BR-006)
     * @return true se cancelou; false se idempotente
     */
    public boolean cancel(User user, String reason) {
        if (this.status == OrderStatus.CANCELLED) {
            return false; // Idempotente
        }
        if (this.status == OrderStatus.COMPLETED) {
            throw new InvalidOrderStateTransitionException("Não é possível cancelar uma ordem que já foi concluída.");
        }
        if (reason == null || reason.trim().length() < 10 || reason.trim().length() > 300) {
            throw new BusinessValidationException("A justificativa de cancelamento deve ter entre 10 e 300 caracteres.");
        }

        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason.trim();
        this.cancelledAt = Instant.now();
        return true;
    }

    public boolean isTerminal() {
        return this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED;
    }

    private void validateTechnicianOwnership(Long technicianId, String actionDescription) {
        if (this.assignedTechnician == null || !this.assignedTechnician.getId().equals(technicianId)) {
            throw new OrderAccessDeniedException(
                    "Apenas o técnico responsável designado pode " + actionDescription + " desta ordem de serviço."
            );
        }
    }

    // =========================================================================
    // Getters and Setters explícitos
    // =========================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(User assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public String getSolutionDescription() {
        return solutionDescription;
    }

    public void setSolutionDescription(String solutionDescription) {
        this.solutionDescription = solutionDescription;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
