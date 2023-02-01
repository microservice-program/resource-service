package app.music.resourceservice.service.model.response;

import java.util.List;

public record RecordIds(List<Long> ids) {
    public void addElement(Long id) {
        ids.add(id);
    }
}
