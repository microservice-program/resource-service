package app.music.resourceservice.service;

import app.music.resourceservice.entity.MusicResource;
import app.music.resourceservice.event.KafkaManager;
import app.music.resourceservice.event.ResourceEvent;
import app.music.resourceservice.exceptions.exception.NotFoundException;
import app.music.resourceservice.exceptions.exception.UnexpectedException;
import app.music.resourceservice.repo.MusicResourceRepo;
import app.music.resourceservice.service.model.response.MusicResourceDto;
import app.music.resourceservice.service.model.response.RecordId;
import app.music.resourceservice.service.model.response.RecordIds;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private static final String S3_BUCKET_NAME = "music-app";
    private static final int READ_LIMIT = 1024 * 1024 * 1024;
    private final AmazonS3 s3;
    private final MusicResourceRepo musicResourceRepo;
    private final KafkaManager kafkaManager;

    @Transactional
    @Override
    public RecordId createResource(MultipartFile file) {
        String filename = file.getOriginalFilename();
        //validations
        checkBucketExists();
        //update image data form db
        MusicResource musicResource = MusicResource.builder()
                .name(filename)
                .build();
        musicResource = musicResourceRepo.save(musicResource);

        //saving image to s3 bucket
        var metadata = new ObjectMetadata();
        metadata.addUserMetadata("Name", filename);
        metadata.addUserMetadata("ResourceId", String.valueOf(musicResource.getId()));
        metadata.setContentType(file.getContentType());
        try {
            byte[] bytes = IOUtils.toByteArray(file.getInputStream());
            metadata.setContentLength(bytes.length);
            var byteArrayInputStream = new ByteArrayInputStream(bytes);
            var request = new PutObjectRequest(S3_BUCKET_NAME,
                    filename,
                    byteArrayInputStream,
                    metadata);
            PutObjectResult res = s3.putObject(request);
            kafkaManager.publish(new ResourceEvent(musicResource.getId()));
            return new RecordId(musicResource.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw new UnexpectedException(e.getMessage());
        }
    }

    @Override
    public InputStreamResource getMusicById(Long id) {
        var music = musicResourceRepo.findById(id).orElseThrow(() -> new NotFoundException("Music not found"));
        checkBucketExists();
        checkObjectExits(music.getName());
        S3Object file = s3.getObject(S3_BUCKET_NAME, music.getName());
        return new InputStreamResource(file.getObjectContent());
    }

    @Override
    public RecordIds deleteMusic(List<Long> ids) {
        checkBucketExists();
        var res = new RecordIds(new ArrayList<>());
        //TODO: avoid anti-pattern
        List<MusicResourceDto> musics = musicResourceRepo.findAllByIds(ids)
                .stream()
                .map(MusicResource::mapToDto)
                .toList();
        musicResourceRepo.deleteAllById(ids);
        musics.forEach(music -> {
            checkObjectExits(music.name());
            s3.deleteObject(S3_BUCKET_NAME, music.name());
            res.addElement(music.id());
        });
        return res;
    }

    @Override
    public Boolean checkMusicById(Long id) {
        return musicResourceRepo.findById(id).isPresent();
    }

    private void checkBucketExists() {
        if (!s3.doesBucketExistV2(S3_BUCKET_NAME)) {
            s3.createBucket(S3_BUCKET_NAME);
        }
    }

    private void checkObjectExits(String objectName) {
        if (!s3.doesObjectExist(S3_BUCKET_NAME, objectName)) {
            throw new NotFoundException("File not found");
        }
    }

    private byte[] readBitmap(S3Object s3Object) {
        byte[] objectBytes;
        try (S3ObjectInputStream s3is = s3Object.getObjectContent();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] readBuf = new byte[1024];
            int readLen;
            while ((readLen = s3is.read(readBuf)) > 0) {
                os.write(readBuf, 0, readLen);
            }

            objectBytes = os.toByteArray();
        } catch (AmazonServiceException | IOException e) {
            throw new UnexpectedException(e.getMessage());
        }
        return objectBytes;
    }
}
