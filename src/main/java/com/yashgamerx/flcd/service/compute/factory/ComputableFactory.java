package com.yashgamerx.flcd.service.compute.factory;

import com.yashgamerx.flcd.service.compute.Computable;

public interface ComputableFactory {
    <T extends Computable> T getComputable(ComputableOption option);
}
