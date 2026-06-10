package com.yashgamerx.flcd.service.compute.factory;

import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;

public interface ComputableFactory {
    <T extends ComputeInjectable> T getComputable(ComputableOption option);
}
