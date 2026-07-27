package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Batch;

import java.util.List;

public interface IBatchRepository {
    void saveAll(List<Batch> batches);
}
