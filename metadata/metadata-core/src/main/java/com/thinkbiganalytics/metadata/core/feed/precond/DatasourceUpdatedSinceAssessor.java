/**
 * 
 */
package com.thinkbiganalytics.metadata.core.feed.precond;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;

import com.thinkbiganalytics.metadata.api.datasource.Datasource;
import com.thinkbiganalytics.metadata.api.datasource.DatasourceCriteria;
import com.thinkbiganalytics.metadata.api.op.Dataset;
import com.thinkbiganalytics.metadata.api.sla.DatasourceUpdatedSinceSchedule;
import com.thinkbiganalytics.metadata.api.op.ChangeSet;
import com.thinkbiganalytics.metadata.sla.api.AssessmentResult;
import com.thinkbiganalytics.metadata.sla.api.Metric;
import com.thinkbiganalytics.metadata.sla.spi.MetricAssessmentBuilder;
import com.thinkbiganalytics.scheduler.util.CronExpressionUtil;

/**
 *
 * @author Sean Felten
 */
public class DatasourceUpdatedSinceAssessor extends MetadataMetricAssessor<DatasourceUpdatedSinceSchedule> {

    @Override
    public boolean accepts(Metric metric) {
        return metric instanceof DatasourceUpdatedSinceSchedule;
    }

    @Override
    public void assess(DatasourceUpdatedSinceSchedule metric, MetricAssessmentBuilder<ArrayList<Dataset<Datasource, ChangeSet>>> builder) {
        List<Date> dates = CronExpressionUtil.getPreviousFireTimes(metric.getCronExpression(), 2);
        DateTime schedTime = new DateTime(dates.get(0));
//        DateTime prevSchedTime = new DateTime(dates.get(1));
        String name = metric.getDatasourceName();
        DatasourceCriteria crit = getDatasetProvider().datasetCriteria().name(name).limit(1);
        List<Datasource> list = getDatasetProvider().getDatasources(crit);
        ArrayList<Dataset<Datasource, ChangeSet>> result = new ArrayList<>();
        boolean incomplete = false;
        
        if (list.size() > 0) {
            Datasource ds = list.get(0);
            Collection<Dataset<Datasource, ChangeSet>> changes = getDataOperationsProvider().getDatasets(ds.getId());
            
            for (Dataset<Datasource, ChangeSet> cs : changes) {
                if (cs.getTime().isBefore(schedTime)) {
                    break;
                }
                
                for (ChangeSet content : cs.getChanges()) {
                    incomplete |= content.getCompletenessFactor() > 0;
                }
                
                result.add(cs);
            }
        }
        
        builder.metric(metric);

        if (result.size() > 0) {
            builder
                .message(result.size() + " change sets found since the scheduled time of " + schedTime)
                .data(result)
                .result(incomplete ? AssessmentResult.WARNING : AssessmentResult.SUCCESS);
        } else {
            builder
                .message("No change sets found since the scheduled time of " + schedTime)
                .data(result)
                .result(AssessmentResult.FAILURE);
        }
        
    }

}