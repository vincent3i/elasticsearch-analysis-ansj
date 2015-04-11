package org.ansj.elasticsearch.index;

import org.ansj.elasticsearch.index.analysis.AnsjIndexAnalyzerProvider;
import org.ansj.elasticsearch.index.analysis.AnsjQueryAnalyzerProvider;
import org.ansj.elasticsearch.index.tokenizer.AnsjIndexTokenizerFactory;
import org.ansj.elasticsearch.index.tokenizer.AnsjQueryTokenizerFactory;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.analysis.AnalysisModule;

public class AnsjAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {
	
	private static ESLogger logger = Loggers.getLogger("ansj-analyzer");

    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {

    }

    @Override
    public void processAnalyzers(AnalyzersBindings analyzersBindings) {
    	logger.debug("Process analyzers...");
        analyzersBindings.processAnalyzer("ansj_index", AnsjIndexAnalyzerProvider.class);
        analyzersBindings.processAnalyzer("ansj_query", AnsjQueryAnalyzerProvider.class);
        super.processAnalyzers(analyzersBindings);
    }

    @Override
    public void processTokenizers(TokenizersBindings tokenizersBindings) {
    	logger.debug("Process tokenizers...");
		tokenizersBindings.processTokenizer("ansj_index_token", AnsjIndexTokenizerFactory.class);
        tokenizersBindings.processTokenizer("ansj_query_token", AnsjQueryTokenizerFactory.class);
		super.processTokenizers(tokenizersBindings);
    }
}
