package eu.forgeops.api.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
            .content(page.getContent())
            .totalElements(page.getTotalElements())
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .build();
    }
}
