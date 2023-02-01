package app.music.resourceservice.service;

import app.music.resourceservice.service.model.response.RecordId;
import app.music.resourceservice.service.model.response.RecordIds;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    RecordId createResource(MultipartFile file);

    HttpEntity<byte[]> getMusicById(Long id);

    RecordIds deleteMusic(List<Long> ids);
}
