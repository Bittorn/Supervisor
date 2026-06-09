package net.bittorn.supervisor.platform;

import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.platform.services.IPlatformHelper;
import net.bittorn.supervisor.platform.services.PlatformHelper;

import java.util.ServiceLoader;

public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> tClass) {
        final T loadedService = ServiceLoader.load(tClass)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for %s".formatted(tClass.getName())));
        Supervisor.LOGGER.debug("Loaded {} for service {}", loadedService, tClass);
        return loadedService;
    }

}
