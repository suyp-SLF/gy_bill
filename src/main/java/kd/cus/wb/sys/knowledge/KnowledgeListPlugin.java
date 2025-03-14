package kd.cus.wb.sys.knowledge;

import com.alibaba.fastjson.JSONObject;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;

import java.util.Map;

/**
 * 知识库|列表插件
 * @author suyp
 *
 */
public class KnowledgeListPlugin extends AbstractListPlugin {

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        System.out.println("123");
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {

        String key = evt.getOperationKey();
        if ("new".equalsIgnoreCase(key)){
            Map<String, Object> map = evt.getParamsMap();

            String the_tree = getPageCache().get("controlstates");
            JSONObject jo = JSONObject.parseObject(the_tree);

            if (null == jo || null == jo.getJSONObject("treeview") || null == jo.getJSONObject("treeview").getJSONObject("focus") || null == jo.getJSONObject("treeview").getJSONObject("focus").getBoolean("isParent")){
                this.getView().showErrorNotification("请选择左侧类别进行新增操作！");
                evt.setCancel(true);
                return;
            }
            Boolean isparent = jo.getJSONObject("treeview").getJSONObject("focus").getBoolean("isParent");
            String name = jo.getJSONObject("treeview").getJSONObject("focus").getString("text");

            if (isparent){
                this.getView().showErrorNotification("请选择最下级类别，" + name + "不是最下级类别");
                evt.setCancel(true);
            }
        }
        System.out.println("");    }
}
