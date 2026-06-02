package com.yashgamerx.flcd.service.list;

public class EmptyListCheckerImplementation implements EmptyListChecker {
    @Override
    public boolean isEmpty(java.util.List<?> list) {
        return list == null || list.isEmpty();
    }
}
