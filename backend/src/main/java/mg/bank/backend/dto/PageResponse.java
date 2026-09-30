package mg.bank.backend.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Reponse paginee exposee au front.
 *
 * On n'expose pas directement Page<T> : sa serialisation Spring inclut
 * des champs techniques (sort, pageable, ...) inutiles et fait.logger un
 * avertissement sur les versions recentes de Spring Data.
 *
 * @param <T> type d'element
 */
@Getter
@Builder
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
