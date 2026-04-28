package com.homekm.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homekm.common.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public interface EmbeddingService {

    boolean isEnabled();

    /** Returns null if disabled or on error. */
    float[] embed(String text);

    @Configuration
    class Cfg {
        private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

        @Bean
        public EmbeddingService embeddingService(AppProperties props, ObjectMapper mapper) {
            if (!props.getEmbedding().isEnabled() ||
                    props.getEmbedding().getOllamaUrl() == null ||
                    props.getEmbedding().getOllamaUrl().isBlank()) {
                log.info("Embeddings: NoopEmbeddingService (set EMBEDDING_ENABLED=true and OLLAMA_URL to enable)");
                return new NoopEmbeddingService();
            }
            log.info("Embeddings: OllamaEmbeddingService model={} url={}",
                    props.getEmbedding().getModel(), props.getEmbedding().getOllamaUrl());
            return new OllamaEmbeddingService(props, mapper);
        }
    }

    class NoopEmbeddingService implements EmbeddingService {
        @Override public boolean isEnabled() { return false; }
        @Override public float[] embed(String text) { return null; }
    }

    class OllamaEmbeddingService implements EmbeddingService {
        private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);
        private final AppProperties props;
        private final ObjectMapper mapper;
        private final RestTemplate http = new RestTemplate();

        OllamaEmbeddingService(AppProperties props, ObjectMapper mapper) {
            this.props = props;
            this.mapper = mapper;
        }

        @Override public boolean isEnabled() { return true; }

        @Override
        public float[] embed(String text) {
            try {
                Map<String, Object> body = Map.of("model", props.getEmbedding().getModel(), "prompt", text);
                @SuppressWarnings("unchecked")
                Map<String, Object> resp = http.postForObject(
                        props.getEmbedding().getOllamaUrl() + "/api/embeddings", body, Map.class);
                if (resp == null) return null;
                Object emb = resp.get("embedding");
                if (!(emb instanceof List<?> list)) return null;
                float[] out = new float[list.size()];
                for (int i = 0; i < list.size(); i++) out[i] = ((Number) list.get(i)).floatValue();
                return out;
            } catch (Exception e) {
                log.warn("Ollama embed failed: {}", e.getMessage());
                return null;
            }
        }
    }
}
