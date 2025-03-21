package kd.cus.wb.ap.busbill;

import kd.bos.bill.BillShowParameter;
import kd.bos.form.FormShowParameter;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.events.BeforeShowBillFormEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;

import java.util.List;
import java.util.Map;

/**
 * 暂估应付单|列表插件，冲回应付分开
 * @author suyp
 *
 */
public class BusbillListPlugin extends AbstractListPlugin {
    @Override
    public void setFilter(SetFilterEvent e) {
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        Map<String, Object> params = formShowParameter.getCustomParams();
        if (null != params && null != params.get("filterch")){
            List<QFilter> qFilter = e.getCustomQFilters();
            qFilter.add(new QFilter("isadjust", QCP.equals,Boolean.parseBoolean(params.get("filterch").toString())));
            e.setCustomQFilters(qFilter);
        }
        System.out.println();
    }

    @Override
    public void beforeShowBill(BeforeShowBillFormEvent e) {

        BillShowParameter parameter = e.getParameter();
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        Map<String, Object> params = formShowParameter.getCustomParams();
        if (null != params && null != params.get("filterch")){
            if (Boolean.parseBoolean(params.get("filterch").toString())){
                parameter.setCaption("暂估冲回单");
            }else{
                parameter.setCaption("暂估应付单");
            }
        }
    }
}
