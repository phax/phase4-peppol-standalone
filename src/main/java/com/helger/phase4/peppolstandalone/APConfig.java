package com.helger.phase4.peppolstandalone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.config.fallback.IConfigWithFallback;
import com.helger.phase4.config.AS4Configuration;

@Immutable
public final class APConfig
{
  private APConfig ()
  {}

  @Nonnull
  private static IConfigWithFallback _getConfig ()
  {
    return AS4Configuration.getConfig ();
  }

  @Nullable
  public static String getMyPeppolSeatID ()
  {
    return _getConfig ().getAsString ("peppol.seatid");
  }

  @Nullable
  public static String getMySmpUrl ()
  {
    return _getConfig ().getAsString ("smp.url");
  }
}
