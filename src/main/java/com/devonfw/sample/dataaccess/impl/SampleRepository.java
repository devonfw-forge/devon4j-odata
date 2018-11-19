package com.devonfw.sample.dataaccess.impl;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.devonfw.sample.dataaccess.api.SampleEntity;

public interface SampleRepository extends JpaRepository<SampleEntity, Long> {

    List<SampleEntity> findByParent_Id(Long id);

}
