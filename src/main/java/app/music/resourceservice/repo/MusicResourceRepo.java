package app.music.resourceservice.repo;

import app.music.resourceservice.entity.MusicResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MusicResourceRepo extends JpaRepository<MusicResource, Long> {

    @Query("FROM MusicResource WHERE id IN (:ids)")
    List<MusicResource> findAllByIds(@Param("ids") List<Long> ids);
}
