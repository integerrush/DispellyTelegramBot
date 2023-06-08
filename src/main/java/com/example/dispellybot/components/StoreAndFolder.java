package com.example.dispellybot.components;

import javax.mail.Folder;
import javax.mail.Store;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class StoreAndFolder {
  private Store store;
  private Folder folder;

  public StoreAndFolder(Store store, Folder folder) {
    this.store = store;
    this.folder = folder;
  }
}
