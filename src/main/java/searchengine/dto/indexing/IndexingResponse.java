package searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(boolean result) {
        this.result = result;
    }
}
