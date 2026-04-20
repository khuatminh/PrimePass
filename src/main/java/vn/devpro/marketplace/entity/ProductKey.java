package vn.devpro.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    @Builder.Default
    private KeyType keyType = KeyType.account;

    @Column(name = "serial_key", length = 500)
    private String serialKey;

    @Column(name = "account_email", length = 200)
    private String accountEmail;

    @Column(name = "account_password", length = 200)
    private String accountPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private KeyStatus status = KeyStatus.available;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "order_item_id")
    private Integer orderItemId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum KeyType {
        serial_key, account
    }

    public enum KeyStatus {
        available, sold, reserved
    }
}
