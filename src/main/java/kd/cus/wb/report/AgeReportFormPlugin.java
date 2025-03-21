package kd.cus.wb.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.datamodel.events.BizDataEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.HyperLinkClickEvent;
import kd.bos.form.events.HyperLinkClickListener;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeFilterF7SelectEvent;
import kd.bos.list.LinkQueryPkId;
import kd.bos.list.LinkQueryPkIdCollection;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QFilter;
import kd.bos.report.ReportList;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.fi.arapcommon.helper.DynamicObjectHelper;
import kd.fi.arapcommon.helper.OrgHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class AgeReportFormPlugin extends AbstractReportFormPlugin implements HyperLinkClickListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        ReportList list = (ReportList) this.getControl("reportlistap");
        list.addHyperClickListener(this);

        Toolbar toolbar = this.getControl("bfgy_advcontoolbarap");

        toolbar.addItemClickListener(this);

        BasedataEdit orgEdit = (BasedataEdit) this.getControl("bfgy_billtypes");
        orgEdit.addBeforeF7SelectListener((listener) -> {
            String range = this.getModel().getValue("bfgy_range").toString();
            ListShowParameter formShowParameter = (ListShowParameter) listener.getFormShowParameter();
            List<String> numbers = new ArrayList<>();
            if ("ap_finapbill".equalsIgnoreCase(range)) {
                numbers.add("WBApFin_pur_BT_S_01");
                numbers.add("WBApFin_other_BT_S_02");
            } else if ("ar_finarbill".equalsIgnoreCase(range)) {
                numbers.add("WBarfin_standard_BT_S_01");
                numbers.add("WBarfin_other_BT_S_02");
            } else if ("ar_revcfmbill".equalsIgnoreCase(range)) {
                numbers.add("WBar_revcfmbill_sale_BT_S_01");
            } else if ("cas_paybill".equalsIgnoreCase(range)) {
                numbers.add("WBcas_paybill_pur_BT_S_01");
                numbers.add("WBcas_paybill_other_BT_S_02");
            } else if ("cas_recbill".equalsIgnoreCase(range)) {
                numbers.add("WBcas_recbill_BT_01");
                numbers.add("WBcas_recbill_BT_02");
            }
            formShowParameter.getListFilterParameter().setFilter(new QFilter("number", "in", numbers));
        });
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        rebacktimerange();
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
    }

    private void updatejson(){
        DynamicObjectCollection timeranges = this.getModel().getEntryEntity("bfgy_timeranges");

        JSONArray timerangesArray = new JSONArray();
        int errortime = 0;
        for (DynamicObject timerange : timeranges) {
            String groupname = timerange.getString("bfgy_groupname");
            Integer num = timerange.getInt("bfgy_num");
            String tip = timerange.getString("bfgy_tip");
            if (StringUtils.isNotBlank(groupname) && num != null) {
                JSONObject jsonobject = new JSONObject();
                jsonobject.put("num", num);
                jsonobject.put("groupname", groupname);
                timerangesArray.add(jsonobject);
            } else {
                errortime++;
//                this.getView().showTipNotification("存在无效时间段！");
            }
        }

//        if (errortime > 0)
//            this.getView().showTipNotification("存在无效时间段！");

        this.getModel().setValue("bfgy_timeranges_text", timerangesArray.toJSONString());
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        IDataEntityProperty property = e.getProperty();
        ChangeData[] changes = e.getChangeSet();
        String range = (String) this.getModel().getValue("bfgy_range");

        updatejson();

        if ("bfgy_betweentype".equalsIgnoreCase(property.getName())) {
            for (ChangeData change : changes) {
                if ("supplier".equalsIgnoreCase(change.getNewValue().toString())) {
                    this.getView().setVisible(true, "bfgy_supplier");
                    this.getView().setVisible(false, "bfgy_customer", "bfgy_user", "bfgy_company");
                } else if ("customer".equalsIgnoreCase(change.getNewValue().toString())) {
                    this.getView().setVisible(true, "bfgy_customer");
                    this.getView().setVisible(false, "bfgy_supplier", "bfgy_user", "bfgy_company");
                } else if ("user".equalsIgnoreCase(change.getNewValue().toString())) {
                    this.getView().setVisible(true, "bfgy_user");
                    this.getView().setVisible(false, "bfgy_customer", "bfgy_supplier", "bfgy_company");
                } else if ("company".equalsIgnoreCase(change.getNewValue().toString())) {
                    this.getView().setVisible(true, "bfgy_company");
                    this.getView().setVisible(false, "bfgy_customer", "bfgy_user", "bfgy_supplier");
                } else {
                    this.getView().setVisible(false, "bfgy_company", "bfgy_customer", "bfgy_user", "bfgy_supplier");
                }
            }
        } else if ("bfgy_range".equalsIgnoreCase(property.getName())) {
            for (ChangeData change : changes) {
                String oldvalue = (String) change.getOldValue();
                String newvalue = (String) change.getNewValue();
                if (StringUtils.isBlank(newvalue)) {
                    this.getView().showTipNotification("范围不允许为空！");
                    this.getModel().setValue("bfgy_range", oldvalue);
                }
            }
        }else if ("bfgy_haveother".equalsIgnoreCase(property.getName())){
            for (ChangeData change : changes) {
//                String oldvalue = (String) change.getOldValue();
                Boolean newvalue = (Boolean) change.getNewValue();
                if (newvalue) {
                    this.getView().setVisible(true, "bfgy_othername");
                }else {
                    this.getView().setVisible(false, "bfgy_othername");
                }
            }
        }
        Control billtypes = this.getControl("bfgy_billtypes");
        System.out.println(1);
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        if ("bfgy_reback".equalsIgnoreCase(evt.getItemKey())){
            rebacktimerange();
        }
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        System.out.println(1);
    }

    @Override
    public void filterContainerBeforeF7Select(BeforeFilterF7SelectEvent args) {
        super.filterContainerBeforeF7Select(args);
    }

    @Override
    public void hyperLinkClick(HyperLinkClickEvent hyperLinkClickEvent) {
        List<Object> ids = new ArrayList<>();
        DynamicObject this_dy = hyperLinkClickEvent.getRowData();
        String key = this_dy.getString("key");

        String range = (String) this.getModel().getValue("bfgy_range");

        ids = AgeListDataPlugin.getColIds(key, hyperLinkClickEvent.getFieldName());

        if (ids.size() > 0) {
            ListShowParameter parameter = new ListShowParameter();
            parameter.setFormId("bos_list");
            parameter.setBillFormId(AgeListDataPlugin.viewMap.get(range));
            parameter.setCaption("相应的" + AgeListDataPlugin.billname_map.get(range) + "列表");
            parameter.getOpenStyle().setTargetKey("tabap");
            parameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
//            ListFilterParameter filters = parameter.getListFilterParameter();
//            filters.setFilter(new QFilter("id", QCP.in,ids));
            LinkQueryPkIdCollection col = parameter.getLinkQueryPkIdCollection();
            ids.forEach(m -> {
                col.add(new LinkQueryPkId(m));
            });
            this.getView().showForm(parameter);
        } else {
            this.getView().showMessage("未找到对应的单据！");
        }
    }

    private void rebacktimerange() {

        this.getModel().setValue("bfgy_unit","year");
        this.getModel().setValue("bfgy_haveother","true");
        this.getModel().setValue("bfgy_othername","5年以上");

        this.getModel().deleteEntryData("bfgy_timeranges");
        this.getModel().batchCreateNewEntryRow("bfgy_timeranges",5);


        this.getModel().setValue("bfgy_groupname","1年以内",0);
        this.getModel().setValue("bfgy_num","1",0);

        this.getModel().setValue("bfgy_groupname","1-2年",1);
        this.getModel().setValue("bfgy_num","1",1);

        this.getModel().setValue("bfgy_groupname","2-3年",2);
        this.getModel().setValue("bfgy_num","1",2);

        this.getModel().setValue("bfgy_groupname","3-4年",3);
        this.getModel().setValue("bfgy_num","1",3);

        this.getModel().setValue("bfgy_groupname","4-5年",4);
        this.getModel().setValue("bfgy_num","1",4);

        updatejson();
    }
}
