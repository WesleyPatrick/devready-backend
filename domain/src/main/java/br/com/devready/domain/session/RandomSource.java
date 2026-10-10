package br.com.devready.domain.session;

@FunctionalInterface
public interface RandomSource {
    int nextInt(int bound);
}
