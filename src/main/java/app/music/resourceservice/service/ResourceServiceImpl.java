package app.music.resourceservice.service;

import app.music.resourceservice.entity.MusicResource;
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
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    private static final String S3_BUCKET_NAME = "music-app";
    private final AmazonS3 s3;
    private final MusicResourceRepo musicResourceRepo;

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
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("Name", filename);
        metadata.addUserMetadata("ResourceId", String.valueOf(musicResource.getId()));
        metadata.setContentType("audio/mpeg");
        try {
            PutObjectRequest request = new PutObjectRequest(S3_BUCKET_NAME, filename, file.getInputStream(), metadata);
            request.setMetadata(metadata);
            s3.putObject(request);
            return new RecordId(musicResource.getId());
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }
    }

    @Override
    public HttpEntity<byte[]> getMusicById(Long id) {
        var music = musicResourceRepo.findById(id).orElseThrow(() -> new NotFoundException("Music not found"));
        checkBucketExists();
        checkObjectExits(music.getName());

        S3Object file = s3.getObject(S3_BUCKET_NAME, music.getName());
        String contentType = file.getObjectMetadata().getContentType();
        byte[] bytes = this.readBitmap(file);
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.valueOf(contentType));
        header.setContentLength(bytes.length);
        header.add("content-disposition", "attachment; filename=\"" + music.getName() + "\"");

        return new HttpEntity<>(bytes, header);
    }

    @Override
    public RecordIds deleteMusic(List<Long> ids) {
        checkBucketExists();
        var res = new RecordIds(new ArrayList<>());
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
