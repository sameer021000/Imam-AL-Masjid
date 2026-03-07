# 📜 Changelog

All notable changes to **Imam AL Masjid** will be documented in this file.

---

## [Unreleased]

### ✨ Added
- **Splash Screen** with smooth transition to the main dashboard.
- **Responsive Layout Scaling** using custom utility for consistent UI across screen sizes.
- **Dynamic Text Scaling** to maintain readability on different Android devices.
- **Clean Modern UI** built using Material components and ConstraintLayout.
- **Added Dark Theme**
- **Login Screen** Implementation with modern claymorphic UI design.
- **Masjid Selection** Dropdown using custom PopupWindow component.
- **Password Visibility** Toggle for secure password input control.
- **Dynamic Login** Validation Logic with credential checking and Masjid selection verification.
- **Login Button** Loading State with temporary disabled state during authentication.
- **Error Messaging System** to display invalid login credentials or missing Masjid selection.
- **Sequential Entrance Animations** for login UI elements.
- **Focus Animations** for Input Fields with subtle scaling and inset shadow effects.
- **Custom Dropdown Item** Layout for Masjid selection.
- **Keyboard Auto-Dismiss** when opening dropdown menu.
- **Navigation Flow** from Splash Screen → Login Screen → Main Dashboard.
- **Masjid Details Viewer** – Press and hold a masjid in the dropdown list to view detailed information.
- **Masjid Details Dialog** displaying masjid name and address.
- **Copy Address Feature** allowing users to quickly copy the masjid address to clipboard.
- **Open in Google Maps** button to launch map navigation for the selected masjid.
- **Animated Dialog Popup** with scale and fade entrance animation.

### 🎨 Improved
- **Optimized Layout Structure** for better performance and responsiveness.
- **Centralized Scaling Utility** for maintaining consistent dimensions across the application.
- **Claymorphic Design System** with raised and inset surfaces for inputs, cards, and buttons.
- **Improved Input Field Alignment** and Spacing using dynamic layout scaling.
- **Enhanced Visual Feedback** when interacting with login fields and dropdown selections.
- **Login Loading Experience** improved using overlay ProgressBar instead of button text change.

### 🐞 Fixed
- Minor layout alignment adjustments.
- Password field typeface reset when toggling visibility.
- Cursor positioning issue after password visibility toggle.
- Code cleanup and refactoring for better maintainability.