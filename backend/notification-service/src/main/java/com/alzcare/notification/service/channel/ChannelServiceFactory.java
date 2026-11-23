package com.alzcare.notification.service.channel;

import com.alzcare.notification.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for resolving notification channel implementations.
 * <p>Provides methods to get the appropriate {@link NotificationChannel} implementation
 * based on the channel type.</p>
 */
@Component
@RequiredArgsConstructor
public class ChannelServiceFactory {

    private final List<NotificationChannelService> channels;

    /**
     * Gets the channel implementation for the specified channel type.
     *
     * @param channelType the {@link NotificationChannel} to resolve
     * @return the channel implementation
     * @throws IllegalArgumentException if no channel is found for the type
     */
    public NotificationChannelService getChannel(NotificationChannel channelType) {
        return channels.stream()
                .filter(channel -> channel.getChannelType() == channelType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No channel found for type: " + channelType));
    }
}
