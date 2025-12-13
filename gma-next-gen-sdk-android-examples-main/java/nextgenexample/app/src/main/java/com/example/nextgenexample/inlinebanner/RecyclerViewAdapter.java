package com.example.nextgenexample.inlinebanner;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.databinding.InlineBannerAdBinding;
import com.example.nextgenexample.databinding.RecyclerViewMenuItemBinding;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import java.util.List;

/**
 * The [RecyclerViewAdapter] class.
 * The adapter provides access to the items in the [MenuItemViewHolder] or the [BannerAdHolder].
 */
public final class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  // A menu item view type.
  private static final int MENU_ITEM_VIEW_TYPE = 0;
  // The banner ad view type.
  private static final int BANNER_AD_VIEW_TYPE = 1;
  // An Activity.
  private final FragmentActivity activity;
  // The recyclerViewItems list contains only [InlineMenuItem] and [BannerAd] types.
  private final List<Object> recyclerViewItems;

  private RecyclerViewAdapter(FragmentActivity activity, List<Object> recyclerViewItems) {
    this.activity = activity;
    this.recyclerViewItems = recyclerViewItems;
  }

  /** Return a new instance of [RecyclerViewAdapter]. */
  public static RecyclerViewAdapter newInstance(FragmentActivity activity, List<Object> recyclerViewItems) {
    return new RecyclerViewAdapter(activity, recyclerViewItems);
  }

  private static class MenuItemViewHolder extends RecyclerView.ViewHolder {
    private final RecyclerViewMenuItemBinding binding;

    MenuItemViewHolder(RecyclerViewMenuItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    TextView getMenuItemName() {
      return binding.menuItemName;
    }
    TextView getMenuItemDescription() {
      return binding.menuItemDescription;
    }
    TextView getMenuItemPrice() {
      return binding.menuItemPrice;
    }
    TextView getMenuItemCategory() {
      return binding.menuItemCategory;
    }
    ImageView getMenuItemImage() {
      return binding.menuItemImage;
    }
  }

  private static class BannerAdHolder extends RecyclerView.ViewHolder {
    private final InlineBannerAdBinding binding;

    BannerAdHolder(InlineBannerAdBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    public CardView getBannerViewContainer() {
      return binding.bannerViewContainer;
    }
  }

  @Override
  public int getItemCount() {
    return recyclerViewItems.size();
  }

  /**
   * Determines the view type for the given position.
   */
  public int getItemViewType(int position) {
    if (recyclerViewItems.get(position) instanceof BannerItem) {
      return BANNER_AD_VIEW_TYPE;
    } else {
      return MENU_ITEM_VIEW_TYPE;
    }
  }

  /**
   * Creates a new view for a menu item view or a banner ad view based on the viewType. This method
   * is invoked by the layout manager.
   */
  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

    if (viewType == MENU_ITEM_VIEW_TYPE) {
      RecyclerViewMenuItemBinding binding =
          RecyclerViewMenuItemBinding.inflate(inflater, viewGroup, false);
      return new MenuItemViewHolder(binding);
    } else {
      InlineBannerAdBinding binding =
          InlineBannerAdBinding.inflate(inflater, viewGroup, false);
      return new BannerAdHolder(binding);
    }
  }

  /**
   * Replaces the content in the views that make up the menu item view and the banner ad view. This
   * method is invoked by the layout manager.
   */
  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    Object item = recyclerViewItems.get(position);

    if (holder instanceof MenuItemViewHolder menuItemHolder
        && item instanceof FoodMenuItem menuItem) {
      // Get the menu item image resource ID.
      String imageName = menuItem.imageName();
      int imageResID =
          activity.getResources().getIdentifier(imageName, "drawable", activity.getPackageName());

      // Add the menu item details to the menu item view.
      menuItemHolder.getMenuItemImage().setImageResource(imageResID);
      menuItemHolder.getMenuItemName().setText(menuItem.name());
      menuItemHolder.getMenuItemPrice().setText(menuItem.price());
      menuItemHolder.getMenuItemCategory().setText(menuItem.category());
      menuItemHolder.getMenuItemDescription().setText(menuItem.description());

    } else if (holder instanceof BannerAdHolder bannerHolder
        && item instanceof BannerItem bannerItem) {
      if (bannerItem.bannerAd != null) {
        // Get the CardView container from the AdViewHolder.
        CardView bannerViewContainer = bannerHolder.getBannerViewContainer();

        // Add the banner ad view to the CardView container.
        BannerAd bannerAd = bannerItem.bannerAd;
        bannerViewContainer.addView(bannerAd.getView(activity));
      }
    } else {
      Log.e(Constant.TAG, "Invalid type at position: " + position);
    }
  }

  @Override
  public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
    if (holder instanceof BannerAdHolder bannerHolder) {
      // Detach the banner ad from the recycled view holder.
      CardView bannerViewContainer = bannerHolder.getBannerViewContainer();
      if (bannerViewContainer != null) {
        bannerViewContainer.removeAllViews();
      }
    }
  }
}
