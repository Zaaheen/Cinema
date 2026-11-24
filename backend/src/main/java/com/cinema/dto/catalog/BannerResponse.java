package com.cinema.dto.catalog;

import com.cinema.entity.Banner;

public record BannerResponse(Long id, String title, String subtitle, String imageUrl,
        String targetPath, int displayOrder) {
    public static BannerResponse from(Banner banner) {
        return new BannerResponse(banner.getId(), banner.getTitle(), banner.getSubtitle(),
                banner.getImageUrl(), banner.getTargetPath(), banner.getDisplayOrder());
    }
}
