package kd.cus.wb.cashier.exsettlement;

import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;

import java.util.EventObject;

/**
 * 结算申请单列表插件
 * @author suyp
 *
 */
public class ExsettlementListplugin extends AbstractListPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        Toolbar repairDataBtnBar = this.getControl("Toolbar");
        repairDataBtnBar.addItemClickListener(this);
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        String key = evt.getItemKey();
        if ("wbother".equals(key)) { //万宝其他应付
            this.getPageCache().put("billtype", "WBApFin_other_BT_S_02");
            this.getView().invokeOperation("new");
        }else if ("wbpur".equals(key)) { //万宝采购应付
            this.getPageCache().put("billtype", "WBApFin_pur_BT_S_01");
            this.getView().invokeOperation("new");
        }

    }
}
