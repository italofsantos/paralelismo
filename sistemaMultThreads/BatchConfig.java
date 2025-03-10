package br.com.sippe.issuer.file.reader.job;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import br.com.sippe.issuer.file.reader.model.dto.ItemCompositeDTO;
import br.com.sippe.issuer.file.reader.model.dto.MsgDTO;
import br.com.sippe.issuer.file.reader.processor.Processor;
import br.com.sippe.issuer.file.reader.reader.MsgReader;

@Configuration
@EnableBatchProcessing
@EnableBatchIntegration
@PropertySource("classpath:application.properties")
public class BatchConfig extends DefaultBatchConfigurer {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Autowired
    public BatchConfig(final JobBuilderFactory jobBuilderFactory, final StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Override
    @Autowired
    public void setDataSource(@Qualifier("batchDataSource") DataSource batchDataSource) {
        super.setDataSource(batchDataSource);
    }

    @Bean
    public Job job(@Qualifier("stepLeituraArquivo") Step leituraArquivoStep, JobParametersIncrementer incrementer) {
        return jobBuilderFactory
            .get("jobLeituraArquivo")
            .start(leituraArquivoStep)
            .incrementer(new RunIdIncrementer())
            .build();
    }

    @Bean
    public Step stepLeituraArquivo(@Value("${spring.batch.chunk.size}") final Integer chunkSize,
                                   MsgReader readerMsg,
                                   Processor processor,
                                   CompositeItemWriter<ItemCompositeDTO> compositeWriter
                                   ,TaskExecutor taskExecutor
                                   ) {
        return stepBuilderFactory
                .get("stepLeituraArquivo")
                .<MsgDTO, ItemCompositeDTO>chunk(10)
                .reader(readerMsg)
                .processor(processor)
                .writer(compositeWriter)
                .taskExecutor(taskExecutor)   
                .throttleLimit(20)             
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5);         
        taskExecutor.setMaxPoolSize(5);         
        taskExecutor.setQueueCapacity(1000);       
        taskExecutor.setThreadNamePrefix("batch-thread-");
        taskExecutor.initialize();
        return taskExecutor;
    }
}
