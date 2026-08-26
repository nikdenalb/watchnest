package dev.watchnest.plannerapp.cms.api;

import dev.watchnest.plannerapp.catalog.CatalogFacade;
import dev.watchnest.plannerapp.cms.api.dto.CatalogTitleListResponse;
import dev.watchnest.plannerapp.cms.api.dto.CatalogTitleRequest;
import dev.watchnest.plannerapp.cms.api.dto.CatalogTitleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/cms/api/v1/titles")
@Tag(name = "CMS Titles", description = "Owned catalog titles")
public class CmsTitleApiController {

    private final CatalogFacade catalogFacade;

    public CmsTitleApiController(CatalogFacade catalogFacade) {
        this.catalogFacade = catalogFacade;
    }

    @GetMapping
    @Operation(summary = "List catalog titles, optionally filtered by English-name substring")
    public CatalogTitleListResponse list(@RequestParam(name = "q", required = false) String q) {
        return new CatalogTitleListResponse(
                catalogFacade.search(q).stream().map(CatalogTitleResponse::from).toList()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a catalog title")
    public CatalogTitleResponse get(@PathVariable UUID id) {
        return CatalogTitleResponse.from(catalogFacade.get(id));
    }

    @PostMapping
    @Operation(summary = "Create a catalog title")
    public ResponseEntity<CatalogTitleResponse> create(@Valid @RequestBody CatalogTitleRequest request) {
        CatalogTitleResponse body = CatalogTitleResponse.from(catalogFacade.create(
                request.type(),
                request.nameEn(),
                request.nameOriginal(),
                request.year(),
                request.description(),
                request.genres(),
                request.countries()
        ));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a catalog title")
    public CatalogTitleResponse update(@PathVariable UUID id, @Valid @RequestBody CatalogTitleRequest request) {
        return CatalogTitleResponse.from(catalogFacade.update(
                id,
                request.type(),
                request.nameEn(),
                request.nameOriginal(),
                request.year(),
                request.description(),
                request.genres(),
                request.countries()
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard-delete a catalog title")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        catalogFacade.delete(id);
        return ResponseEntity.noContent().build();
    }
}
