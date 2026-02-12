package com.starterpack.backend.modules.upload.infrastructure;

import java.util.UUID;

import com.starterpack.backend.modules.upload.domain.UploadMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadMediaRepository extends JpaRepository<UploadMedia, UUID> {
}
