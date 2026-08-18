package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.dto.AntropometriaRequest;
import br.edu.nutriclinica.dto.AntropometriaResponse;
import br.edu.nutriclinica.service.AntropometriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atendimentos/{id}/antropometria")
@Tag(name = "Seções do prontuário")
public class AntropometriaController {

    private final AntropometriaService antropometriaService;

    public AntropometriaController(AntropometriaService antropometriaService) {
        this.antropometriaService = antropometriaService;
    }

    /**
     * O {@code hasRole} barra supervisor e admin já na borda; que o estagiário
     * seja o dono do atendimento e que o status admita escrita é decidido no
     * serviço de domínio, onde a regra vale para qualquer chamador.
     */
    @PatchMapping
    @PreAuthorize("hasRole('ESTAGIARIO')")
    @Operation(summary = "Salva a antropometria e retorna os indicadores calculados",
            description = "IMC, classificação e relação cintura/quadril são calculados pelo "
                    + "servidor. Campos calculados enviados pelo cliente são ignorados.")
    public ResponseEntity<AntropometriaResponse> salvar(@PathVariable Long id,
                                                        @Valid @RequestBody AntropometriaRequest requisicao) {
        return ResponseEntity.ok(antropometriaService.salvar(id, requisicao));
    }
}
