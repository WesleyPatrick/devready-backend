package br.com.devready.api.identity.dto;

import br.com.devready.domain.identity.SocialProvider;
import java.util.List;

public record AuthProvidersResponse(List<SocialProvider> providers) {}
