package org.example.vendly_remastered.repository;

import java.time.LocalDate;

public interface IArchiveRepository {
    public int ArchiveSales(String path, LocalDate beginningDate , LocalDate endDate);
}
