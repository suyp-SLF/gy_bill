package kd.cus.wb.workflow;

import kd.bos.bec.model.KDBizEvent;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.workflow.api.AgentExecution;
import kd.bos.workflow.api.AgentTask;
import kd.bos.workflow.api.IBusinessContext;
import kd.bos.workflow.api.constants.WFTaskResultEnum;
import kd.bos.workflow.engine.extitf.WorkflowPlugin;
import kd.bos.workflow.exception.WFTaskException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 出口发票工作流配置|根据项目号找财务经理
 * @author suyp
 */
public class ExportinvoiceWorkflowPlugin extends WorkflowPlugin {

    private static final String PRO_BILL_LOGO = "bfgy_proj_wb_pmb";

    @Override
    public List<Long> calcUserIds(AgentExecution execution) {
        String key = execution.getBusinessKey();//单据id
        String logo = execution.getEntityNumber();
        DynamicObject this_dy = BusinessDataServiceHelper.loadSingle(key, logo);
        if(null != this_dy){
            String billno = this_dy.getString("bfgy_projectno");
            QFilter[] qFilters = {new QFilter("bfgy_projno", QCP.equals,billno)};
            DynamicObject pro_dy = BusinessDataServiceHelper.loadSingle(PRO_BILL_LOGO, "bfgy_projno,bfgy_userfield", qFilters);
            if (null != pro_dy){
                DynamicObject money_person = pro_dy.getDynamicObject("bfgy_userfield");
                if (null != money_person){
                    return Arrays.asList((Long)money_person.getPkValue());
                }
            }
        }
        return null;

    }
}
