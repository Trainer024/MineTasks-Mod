package com.notetask.util;

import com.notetask.data.ItemTask;
import com.notetask.data.SaveData;
import com.notetask.data.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TaskItemFilters {

    private TaskItemFilters() {}

    /** Item IDs already used by other item tasks (optionally keep one id, e.g. when editing). */
    public static Set<String> usedItemIds(String keepItemId) {
        Set<String> ids = new HashSet<>();
        for (Task task : SaveData.getTasks()) {
            if (task instanceof ItemTask it) {
                if (keepItemId != null && keepItemId.equals(it.itemId)) {
                    continue;
                }
                ids.add(it.itemId);
            }
        }
        return ids;
    }

    public static List<ItemMatch> excludeAlreadyUsed(List<ItemMatch> matches, String keepItemId) {
        Set<String> used = usedItemIds(keepItemId);
        if (used.isEmpty()) {
            return matches;
        }
        List<ItemMatch> out = new ArrayList<>(matches.size());
        for (ItemMatch m : matches) {
            if (!used.contains(m.idString())) {
                out.add(m);
            }
        }
        return out;
    }
}
