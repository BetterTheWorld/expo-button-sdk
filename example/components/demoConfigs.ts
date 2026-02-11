import { Alert, Dimensions } from "react-native";
import type { StartPurchasePathOptions } from "@flipgive/expo-button-sdk";
import { MOCK_PROMOTION_DATA } from "../../src/constants/MockData";

const TOKEN = process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE ?? "";

// Figma frame: 375x812 (standard iPhone)
const { width: screenWidth, height: screenHeight } = Dimensions.get("window");
const figmaScale = (figmaPx: number, figmaBase: number, screenBase: number) =>
  Math.round(screenBase * (figmaPx / figmaBase));
const fw = (px: number) => figmaScale(px, 375, screenWidth);
const fh = (px: number) => figmaScale(px, 812, screenHeight);

export interface DemoConfig {
  id: string;
  title: string;
  options: StartPurchasePathOptions;
}

export const DEMO_CONFIGS: DemoConfig[] = [
  {
    id: "basic",
    title: "Basic Browser",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      footerBackgroundColor: "#FF3453",
      footerTintColor: "#FF3453",
      headerBackgroundColor: "#FF3453",
      headerSubtitle: "Gives 10%",
      headerSubtitleColor: "#FFE599",
      headerTintColor: "#",
      headerTitle: "Gapo",
      headerTitleColor: "#347796",
      exitConfirmation: {
        enabled: true,
        title: "Are you sure you want to leave?",
        message:
          "You might miss out on exclusive offers and lose your progress.",
        stayButtonLabel: "Stay",
        leaveButtonLabel: "Leave",
      },
    },
  },
  {
    id: "pip-default",
    title: "PiP Default (Hide on BG)",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      footerBackgroundColor: "#FF3453",
      footerTintColor: "#FF3453",
      headerBackgroundColor: "#FF3453",
      headerSubtitle: "Gives 10%",
      headerSubtitleColor: "#FFE599",
      headerTintColor: "#",
      headerTitle: "Gapo",
      headerTitleColor: "#347796",
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          hideOnAppBackground: true,
        },
      },
      exitConfirmation: {
        enabled: true,
        title: "Are you sure you want to leave?",
        message:
          "You might miss out on exclusive offers and lose your progress.",
        stayButtonLabel: "Stay",
        leaveButtonLabel: "Leave",
      },
    },
  },
  {
    id: "pip-square",
    title: "PiP Square (YouTube Style)",
    options: {
      url:
        process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE ??
        "https://www.adidas.com/us",
      token: TOKEN,
      footerBackgroundColor: "#FF3453",
      footerTintColor: "#FF3453",
      headerBackgroundColor: "#FF3453",
      headerSubtitle: "Gives 10%",
      headerSubtitleColor: "#FFE599",
      headerTintColor: "#",
      headerTitle: "Gapo",
      headerTitleColor: "#347796",
      animationConfig: {
        pictureInPicture: {
          enabled: true,
        },
      },
      exitConfirmation: {
        enabled: true,
        title: "Are you sure you want to leave?",
        message:
          "You might miss out on exclusive offers and lose your progress.",
        stayButtonLabel: "Stay",
        leaveButtonLabel: "Leave",
      },
    },
  },
  {
    id: "pip-custom",
    title: "PiP Custom Position & Size",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      footerBackgroundColor: "#FF3453",
      footerTintColor: "#FF3453",
      headerBackgroundColor: "#FF3453",
      headerSubtitle: "Gives 10%",
      headerSubtitleColor: "#FFE599",
      headerTintColor: "#",
      headerTitle: "Gapo",
      headerTitleColor: "#347796",
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          position: { x: 50, y: 100 },
          size: { width: 180, height: 120 },
          earnText: "Earn 2%",
        },
      },
      coverImage: {
        uri: "https://placecats.com/millie_neo/300/200",
        scaleType: "stretch",
        backgroundColor: "#1a1a2e",
        padding: 8,
      },
      exitConfirmation: {
        enabled: true,
        title: "Are you sure you want to leave?",
        message:
          "You might miss out on exclusive offers and lose your progress.",
        stayButtonLabel: "Stay",
        leaveButtonLabel: "Leave",
      },
      onClose: () => {
        Alert.alert("close");
      },
    },
  },
  {
    id: "pip-android-small",
    title: "PiP Android (4:3)",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      footerBackgroundColor: "#4CAF50",
      footerTintColor: "#FFFFFF",
      headerBackgroundColor: "#4CAF50",
      headerSubtitle: "Android PiP Test",
      headerSubtitleColor: "#E8F5E9",
      headerTintColor: "#FFFFFF",
      headerTitle: "Small PiP",
      headerTitleColor: "#FFFFFF",
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          earnText: "Earn 3%",
          androidAspectRatio: { width: 3, height: 2 },
        },
      },
      coverImage: {
        uri: "https://placecats.com/millie_neo/300/200",
        scaleType: "cover",
        backgroundColor: "#1a1a2e",
        padding: 8,
      },
      exitConfirmation: {
        enabled: true,
        title: "Are you sure?",
        message: "You might miss out on deals.",
        stayButtonLabel: "Stay",
        leaveButtonLabel: "Leave",
      },
    },
  },
  {
    id: "promotions",
    title: "Browser with Promotions",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      headerTitleColor: "#ffffff",
      headerSubtitleColor: "#ffffff",
      headerBackgroundColor: "#074A7B",
      headerTintColor: "#ffffff",
      footerTintColor: "#ffffff",
      footerBackgroundColor: "#074A7B",
      headerSubtitle: "Gives 10%",
      headerTitle: "Gapo",
      promotionBadgeFontSize: 14,
      promotionBadgeLabel: "2 deals",
      promotionListTitle: "De@ls!",
      promotionData: MOCK_PROMOTION_DATA,
      closeOnPromotionClick: true,
      onPromotionClick: async (promotionId: string) => {
        console.log("Promotion clicked:", promotionId);
        await new Promise((resolve) => setTimeout(resolve, 1000));
        return {
          url: "https://example.com",
          token: "new-token-for-promotion",
        };
      },
      exitConfirmation: {
        enabled: true,
        title: "Leaving so soon?",
        message: "If you exit now, you might miss your cashback.",
        stayButtonLabel: "Keep shopping",
        leaveButtonLabel: "Leave anyway",
      },
      headerLeftIcon:
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAAGUjEkAAAABklEQVQoU2NkYGAAAABQABCA6gQm5gAAAABJRU5ErkJggg==",
      headerRightButtons: [
        {
          icon: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAAGUjEkAAAABklEQVQoU2NkYGAAAABQABCA6gQm5gAAAABJRU5ErkJggg==",
          action: "close",
        },
        { title: "⭐", action: "favorite" },
      ],
      onHeaderButtonClick: (action: string) => {
        console.log("Header Button:", action);
      },
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          position: { x: 20, y: 200 },
          size: { width: 300, height: 450 },
          earnText: "Earn 2%",
        },
      },
    },
  },
  {
    id: "pip-simulated",
    title: "Simulated PiP (No Native)",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      footerBackgroundColor: "#6C63FF",
      footerTintColor: "#FFFFFF",
      headerBackgroundColor: "#6C63FF",
      headerSubtitle: "Simulated PiP Test",
      headerSubtitleColor: "#D4D1FF",
      headerTintColor: "#FFFFFF",
      headerTitle: "Simulated",
      headerTitleColor: "#FFFFFF",
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          useNativePip: false,
          size: { width: 150, height: 150 },
          chevronColor: "#FFFFFF",
          earnText: "Earn 5%",
          earnTextColor: "#FFFFFF",
          earnTextBackgroundColor: "#6C63FF",
        },
      },
      coverImage: {
        uri: "https://placecats.com/millie_neo/300/200",
        scaleType: "cover",
        backgroundColor: "#1a1a2e",
        padding: 8,
      },
      exitConfirmation: {
        enabled: true,
        title: "Are you sure you want to leave?",
        message: "You might miss out on deals.",
        stayButtonLabel: "Stay",
        leaveButtonLabel: "Leave",
      },
      onClose: () => {
        Alert.alert("Browser closed");
      },
    },
  },
  {
    id: "pip-simulated-promos",
    title: "Simulated PiP + Promotions",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      headerTitleColor: "#ffffff",
      headerSubtitleColor: "#ffffff",
      headerBackgroundColor: "#2D3436",
      headerTintColor: "#ffffff",
      footerTintColor: "#ffffff",
      footerBackgroundColor: "#2D3436",
      headerSubtitle: "Gives 10%",
      headerTitle: "Sim PiP + Promos",
      promotionBadgeFontSize: 14,
      promotionBadgeLabel: "2 deals",
      promotionListTitle: "Available Deals",
      promotionData: MOCK_PROMOTION_DATA,
      closeOnPromotionClick: true,
      onPromotionClick: async (promotionId: string) => {
        console.log("Promotion clicked:", promotionId);
        await new Promise((resolve) => setTimeout(resolve, 1000));
        return {
          url: "https://example.com",
          token: "new-token-for-promotion",
        };
      },
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          useNativePip: false,
          position: { x: 20, y: 200 },
          size: { width: 180, height: 180 },
          earnText: "Earn 10%",
          earnTextColor: "#FFFFFF",
          earnTextBackgroundColor: "#00B894",
        },
      },
      coverImage: {
        uri: "https://placecats.com/millie_neo/300/200",
        scaleType: "cover",
        backgroundColor: "#2D3436",
        padding: 8,
      },
      exitConfirmation: {
        enabled: true,
        title: "Leaving so soon?",
        message: "If you exit now, you might miss your cashback.",
        stayButtonLabel: "Keep shopping",
        leaveButtonLabel: "Leave anyway",
      },
    },
  },
  {
    id: "pip-nike",
    title: "PiP Nike (Custom Colors)",
    options: {
      url: "https://www.nike.com",
      token: TOKEN,
      footerBackgroundColor: "#FF3453",
      footerTintColor: "#FF3453",
      headerBackgroundColor: "#FF3453",
      headerSubtitle: "Gives 5%",
      headerSubtitleColor: "#FFE599",
      headerTintColor: "#FFFFFF",
      headerTitle: "Nike Store",
      headerTitleColor: "#FFFFFF",
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          chevronColor: "#FFFFFF",
          earnText: "Earn 5% cashback",
          earnTextColor: "#FFFFFF",
          earnTextBackgroundColor: "#FF3453",
        },
      },
      coverImage: {
        uri: "https://images.unsplash.com/photo-1542291026-7eec264c27ff",
        scaleType: "center",
        backgroundColor: "white",
      },
    },
  },
  {
    id: "pip-ulta",
    title: "PiP Figma Style",
    options: {
      url: "https://www.adidas.com/us",
      token: TOKEN,
      headerBackgroundColor: "#074A7B",
      headerTintColor: "#FFFFFF",
      headerTitle: "ULTA Beauty",
      headerTitleColor: "#FFFFFF",
      headerSubtitle: "Earn 2%",
      headerSubtitleColor: "#B3D4FC",
      footerBackgroundColor: "#074A7B",
      footerTintColor: "#FFFFFF",
      animationConfig: {
        pictureInPicture: {
          enabled: true,
          useNativePip: false,
          size: { width: fw(170), height: fh(104) },
          chevronColor: "#005E9D",
          earnText: "Earn 2%",
          earnTextColor: "#143860",
          earnTextFontFamily: "OpenSans-SemiBold",
          earnTextFontSize: 13,
          earnTextFontWeight: "500",
          earnTextLineHeight: 16,
          pipOverlayInset: 12,
          pipCloseButtonSize: 13,
          pipChevronSize: 17,
          pipChevronHeight: 16,
          pipChevronStrokeWidth: 0.4,
          pipCloseStrokeWidth: 0.1,
          earnTextMargin: 2.5,
        },
      },
      coverImage: {
        uri: "https://placecats.com/millie_neo/300/200",
        scaleType: "contain",
        backgroundColor: "#FFFFFF",
        padding: 22,
      },
      exitConfirmation: {
        enabled: true,
        title: "Are you sure you want to leave?",
        message: "You might miss out on exclusive offers.",
        stayButtonLabel: "Stay",
        leaveButtonLabel: "Leave",
      },
      onClose: () => {
        // Alert.alert("Browser closed");
      },
    },
  },
];
