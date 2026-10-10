package br.com.devready.application.career.usecase;

import br.com.devready.application.career.dto.LearningTrackView;
import br.com.devready.application.career.service.TrackAssembler;
import java.util.UUID;

public class GetLearningTrackUseCase {

    private final TrackAssembler assembler;

    public GetLearningTrackUseCase(TrackAssembler assembler) {
        this.assembler = assembler;
    }

    public LearningTrackView execute(UUID userId) {
        return assembler.assemble(userId);
    }
}
