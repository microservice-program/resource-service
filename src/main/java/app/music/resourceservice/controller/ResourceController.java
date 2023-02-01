package app.music.resourceservice.controller;

import app.music.resourceservice.service.ResourceService;
import app.music.resourceservice.service.model.response.RecordId;
import app.music.resourceservice.service.model.response.RecordIds;
import app.music.resourceservice.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/resources")
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping("/{id}")
    public HttpEntity<byte[]> getMusicById(@PathVariable Long id) {
        return resourceService.getMusicById(id);
    }

    @PostMapping
    public ResponseEntity<RecordId> createResource(@RequestParam MultipartFile file) {
        return ResponseEntity.ok(resourceService.createResource(file));
    }

    @DeleteMapping
    public ResponseEntity<RecordIds> deleteResources(@RequestParam("id") String id) {
        List<Long> ids = RequestUtils.parseStringToListLong(id);
        RecordIds res = resourceService.deleteMusic(ids);
        return ResponseEntity.ok(res);
    }
}


