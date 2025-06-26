package fctreddit.impl.imgur;

import java.util.List;
import java.util.Map;

public class BasicResponseList {
    private List<Map<String, Object>> data;
    private int status;
    private boolean success;

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
