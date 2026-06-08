package com.yashgamerx.flcd.service.precompute.factory;

import com.yashgamerx.flcd.service.precompute.Precomputable;

public interface PreComputeFactory {
    <T extends Precomputable> T getPreComputable(PreComputableOption option);
}
