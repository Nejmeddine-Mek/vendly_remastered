package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Batch;

import java.util.List;

public interface IBatchrepository {
    void saveAll(List<Batch> batches);
}
