package org.example.vendly_remastered.cache;

import javafx.scene.Parent;
import org.example.vendly_remastered.cache.model.PageCacheLine;
import org.example.vendly_remastered.constants.Constants;

public class PagesCache {
    private int ALNCounter;
    private final PageCacheLine[] pagesCache;

    public PagesCache(){
        ALNCounter = 0;
        pagesCache = new PageCacheLine[4];
    }

    public Parent getPage(Constants pageName){
        for (PageCacheLine pageCacheLine : pagesCache) {
            if(pageCacheLine == null)
                continue;
            if (pageName == pageCacheLine.getPageName()) {
                pageCacheLine.setALN(ALNCounter++);
                return pageCacheLine.getPage();
            }

        }
       return null;
    }

    public void addPage(Parent page, Constants pageName, int priority) {
        // PHASE 1: Scan for empty slots first (Startup Phase)
        for (int i = 0; i < pagesCache.length; i++) {
            if (pagesCache[i] == null) {
                pagesCache[i] = new PageCacheLine(ALNCounter++, page, priority, pageName);
                return; // Free space found, work is done!
            }
        }

        // PHASE 2: Cache is 100% full. Loop until a valid victim is evicted.
        while (true) {
            int victimPageIndex = 0;
            int minALN = pagesCache[0].getALN();

            // Find the absolute oldest page in the cache
            for (int i = 1; i < pagesCache.length; i++) {
                if (pagesCache[i].getALN() < minALN) {
                    minALN = pagesCache[i].getALN();
                    victimPageIndex = i;
                }
            }

            // Inspect our candidate
            PageCacheLine candidate = pagesCache[victimPageIndex];

            // Hard Threshold Rule: Priorities 1 and 2 are protected and get a Second Chance
            if (candidate.getPriority() <= 2) {
                candidate.setALN(ALNCounter++); // Grant second chance (push to back of line)
                System.out.println("[Cache] Protected page " + candidate.getPageName() + " granted second chance.");
                // The 'while(true)' loop restarts and scans for the next minimum ALN
            } else {
                // Found a low-priority page (Priority >= 3). Evict it!
                System.out.println("[Cache] Evicting cold page: " + candidate.getPageName());
                pagesCache[victimPageIndex] = new PageCacheLine(ALNCounter++, page, priority, pageName);
                break; // Eviction complete, exit loop
            }
        }
    }
}
