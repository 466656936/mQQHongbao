package qqhongbao.example.administrator.qqhongbao;

/**
 * Created by Administrator on 2017/1/7.
 */

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class QQHongbaoService extends AccessibilityService {


    private boolean QQHongbaoReceived;
    private long lastFetchedTime = 0;
    private String lastFetchedHongbaoId = null;
    private AccessibilityNodeInfo rootNodeInfo;
    private List<AccessibilityNodeInfo> mReceiveNode;


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void recycle(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
//            Log.i("","child widget-----" + info.getClassName());
//            Log.i("", "showDialog:" + info.canOpenPopup());
//            Log.i("", "Text：" + info.getText());
//            Log.i("", "windowId:" + info.getWindowId());
            if (info.getText() != null && info.getText().toString().equals("点击输入口令")) {
                info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            if (info.getClassName().toString().equals("android.widget.Button") && info.getText().toString().equals("发送")) {
                info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }

        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i));
                }
            }
        }
    }
    /*
     * 抢红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        this.rootNodeInfo = event.getSource();
        if (rootNodeInfo == null) {
            return;
        }
        mReceiveNode = null;
        checkNodeInfo();
        /* 如果已经接收到红包并且还没有戳开 */
        if (QQHongbaoReceived && (mReceiveNode != null)) {
            int size = mReceiveNode.size();
            if (size > 0) {
                String id = getHongbaoText(mReceiveNode.get(size - 1));
                long now = System.currentTimeMillis();
                if (this.shouldReturn(id, now - lastFetchedTime))
                    return;
                lastFetchedHongbaoId = id;
                lastFetchedTime = now;
                AccessibilityNodeInfo cellNode = mReceiveNode.get(size - 1);
                if (cellNode.getText().toString().equals("口令红包已拆开")) {
                    return;
                }
                cellNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                //Log.i("", "---------开始----------");
                if (cellNode.getText().toString().equals("口令红包")) {
                    AccessibilityNodeInfo rowNode = getRootInActiveWindow();
                    if (rowNode == null) {
                        //Log.i("", "noteInfo is　null");
                        return;
                    } else {
                        recycle(rowNode);
                    }
                }
                //Log.i("", "-----------结束------------");
                //Log.i("", "text = " + cellNode.getText().toString());
                QQHongbaoReceived = false;
            }
        }
    }


    /**
     * 检查节点信息
     */
    private void checkNodeInfo() {
        if (rootNodeInfo == null) {
            return;
        }
         /* 聊天会话窗口，获取普通红包，口令红包节点，及输入口令节点和发送按钮节点，通过虚拟点击红包以及*/
        List<AccessibilityNodeInfo> nodes1 = this.findAccessibilityNodeInfosByTexts(this.rootNodeInfo, new String[]{
                "点击拆开", "口令红包", "点击输入口令", "发送"});
        if (!nodes1.isEmpty()) {
            String nodeId = Integer.toHexString(System.identityHashCode(this.rootNodeInfo));
            if (!nodeId.equals(lastFetchedHongbaoId)) {
                QQHongbaoReceived = true;
                mReceiveNode = nodes1;
            }
            return;
        }
    }


    /**
     * 将节点对象的id和红包上的内容合并
     * return 红包特征文本
     */
    private String getHongbaoText(AccessibilityNodeInfo node) {
        /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npe) {
            return null;
        }
        return content;
    }

    /**
     * 判断是否返回,减少不必要的点击
     * 红包文本和缓存文本不一致时触发点击
     * 缓存文本一至但超过时间间隔
     */
    private boolean shouldReturn(String id, long duration) {
        // ID为空
        if (id == null) return true;
        // 名称和缓存不一致
        if (duration < 5000 && id.equals(lastFetchedHongbaoId)) {
            return true;
        }
        return false;
    }

    /*
     *获取窗口内的组件信息
     */
    private List<AccessibilityNodeInfo> findAccessibilityNodeInfosByTexts(AccessibilityNodeInfo nodeInfo, String[] texts) {
        for (String text : texts) {
            if (text == null) continue;

            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText(text);

            if (!nodes.isEmpty()) {
                if (text.equals("Open") && !nodeInfo.findAccessibilityNodeInfosByText("You've opened").isEmpty()) {
                    continue;
                }
                return nodes;
            }
        }
        return new ArrayList<>();
    }
    @Override
    public void onInterrupt() {

    }
}
