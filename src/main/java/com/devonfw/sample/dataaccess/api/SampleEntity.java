package com.devonfw.sample.dataaccess.api;

import lombok.*;

import javax.persistence.*;
import java.util.List;

import org.hibernate.annotations.Nationalized;
import com.devonfw.module.odata.common.api.ODataEntity;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "\"db.dbmodel::Sample\""
        , uniqueConstraints = @UniqueConstraint(
        name = SampleEntity.UQ_CHAPTERGROUPING_KEYFIGURE,
        columnNames = "\"KeyFigure\""
)
)
public class SampleEntity implements ODataEntity<Long> {

    public static final String UQ_CHAPTERGROUPING_KEYFIGURE = "UQ_TestEntity_KeyFigure";

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Nationalized
    @Column(name = "\"Identifier\"", length = 256)
    private String identifier;

    @Column(name = "\"KeyFigure\"")
    private String keyFigure;

    @Column(name = "\"Explanation\"", columnDefinition = "NCLOB", length = 2147483647)
    private String explanation;

    @Column(name = "\"Note\"", columnDefinition = "NCLOB", length = 2147483647)
    private String note;

    @Column(name = "\"IsDeleted\"")
    private Boolean isDeleted;

    @ManyToOne
    @JoinColumn(name = "\"AssignedParent_ID\"")
    public SampleEntity parent;

    @Singular
    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    private List<SampleEntity> children;
}
