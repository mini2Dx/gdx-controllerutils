package de.golfgl.gdx.controllers.jamepad.support;

import com.badlogic.gdx.controllers.AdvancedController;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Logger;
import com.studiohartman.jamepad.ControllerAxis;
import com.studiohartman.jamepad.ControllerButton;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import java.util.UUID;

public class JamepadController implements AdvancedController {
    private static final IntMap<ControllerButton> CODE_TO_BUTTON = new IntMap<>(ControllerButton.values().length);
    private static final IntMap<ControllerAxis> CODE_TO_AXIS = new IntMap<>(ControllerAxis.values().length);
    private static final Logger logger = new Logger(JamepadController.class.getSimpleName());

    static {
        for (ControllerButton button : ControllerButton.values()) {
            CODE_TO_BUTTON.put(button.ordinal(), button);
        }

        for (ControllerAxis axis : ControllerAxis.values()) {
            CODE_TO_AXIS.put(axis.ordinal(), axis);
        }
    }

    private final CompositeControllerListener compositeControllerListener = new CompositeControllerListener();
    private final ControllerIndex controllerIndex;
    private final IntMap<Boolean> buttonState = new IntMap<>();
    private final IntMap<Float> axisState = new IntMap<>();
    private final String uuid;
    private boolean connected = true;

    public JamepadController(ControllerIndex controllerIndex) {
        this.controllerIndex = controllerIndex;
        this.uuid = UUID.randomUUID().toString();
        initializeState();
    }

    @Override
    public boolean getButton(final int buttonCode) {
        try {
            ControllerButton button = toButton(buttonCode);
            return button != null && controllerIndex.isButtonPressed(button);
        } catch (ControllerUnpluggedException e) {
            setDisconnected();
        }
        return false;
    }

    @Override
    public float getAxis(final int axisCode) {
        try {
            ControllerAxis axis = toAxis(axisCode);

            if (axis == null) {
                return 0.0f;
            } else {
                float axisState = controllerIndex.getAxisState(axis);

                // Jamepad flips vertical controller axis values. That's not a great idea to differ from
                // common standards. Up is negative, down is positive, that's the way it is and we ensure this here
                if (axis == ControllerAxis.LEFTY || axis == ControllerAxis.RIGHTY)
                    axisState = -axisState;

                return axisState;
            }
        } catch (ControllerUnpluggedException e) {
            setDisconnected();
        }
        return 0f;
    }

    @Override
    public PovDirection getPov(int povCode) {
        // not supported
        return PovDirection.center;
    }

    @Override
    public boolean getSliderX(int sliderCode) {
        // not supported
        return false;
    }

    @Override
    public boolean getSliderY(int sliderCode) {
        // not supported
        return false;
    }

    @Override
    public Vector3 getAccelerometer(int accelerometerCode) {
        // not supported
        return Vector3.Zero;
    }

    @Override
    public void setAccelerometerSensitivity(float sensitivity) {
        // not supported
    }

    @Override
    public String getName() {
        try {
            return controllerIndex.getName();
        } catch (ControllerUnpluggedException e) {
            setDisconnected();
        }
        return "Unknown";
    }

    private void setDisconnected() {
        connected = false;
        logger.info("Failed querying controller at index: " + controllerIndex.getIndex());
        compositeControllerListener.disconnected(this);
    }

    @Override
    public void addListener(ControllerListener listener) {
        compositeControllerListener.addListener(listener);
    }

    @Override
    public void removeListener(ControllerListener listener) {
        compositeControllerListener.removeListener(listener);
    }

    public boolean update() {
        updateButtonsState();
        updateAxisState();
        return connected;
    }

    private ControllerButton toButton(int buttonCode) {
        return CODE_TO_BUTTON.get(buttonCode);
    }

    private ControllerAxis toAxis(int axisCode) {
        return CODE_TO_AXIS.get(axisCode);
    }

    private void updateAxisState() {
        for (ControllerAxis axis : ControllerAxis.values()) {
            int id = axis.ordinal();

            float value = getAxis(id);
            if (value != axisState.get(id)) {
                if (logger.getLevel() == Logger.DEBUG) {
                    logger.debug("Axis [" + id + " - " + toAxis(id) + "] moved [" + value + "]");
                }
                compositeControllerListener.axisMoved(this, id, value);
            }
            axisState.put(id, value);
        }
    }

    private void updateButtonsState() {
        for (ControllerButton button : ControllerButton.values()) {
            int id = button.ordinal();

            boolean pressed = getButton(id);
            if (pressed != buttonState.get(id)) {
                if (pressed) {
                    compositeControllerListener.buttonDown(this, id);
                } else {
                    compositeControllerListener.buttonUp(this, id);
                }

                if (logger.getLevel() == Logger.DEBUG) {
                    logger.debug("Button [" + id + " - " + toButton(id) + "] is " + (pressed ? "pressed" : "released"));
                }
            }
            buttonState.put(id, pressed);
        }
    }

    private void initializeState() {
        for (ControllerAxis axis : ControllerAxis.values()) {
            axisState.put(axis.ordinal(), 0.0f);
        }

        for (ControllerButton button : ControllerButton.values()) {
            buttonState.put(button.ordinal(), false);
        }
    }

    @Override
    public boolean canVibrate() {
        return true;
    }

    @Override
    public boolean isVibrating() {
        return controllerIndex.isVibrating();
    }

    @Override
    public void startVibration(float strength) {
        try {
            controllerIndex.startVibration(strength, strength);
        } catch (ControllerUnpluggedException e) {
            // do nothing
        }
    }

    @Override
    public void stopVibration() {
        controllerIndex.stopVibration();
    }

    @Override
    public String getUniqueId() {
        return uuid;
    }

    @Override
    public boolean supportsPlayerIndex() {
        return false;
    }

    @Override
    public int getPlayerIndex() {
        return PLAYER_IDX_UNSET;
    }

    @Override
    public void setPlayerIndex(int index) {
        // unsupported
    }

    @Override
    public int getMinButtonIndex() {
        return 0;
    }

    @Override
    public int getMaxButtonIndex() {
        return CODE_TO_BUTTON.size - 1;
    }

    @Override
    public int getAxisCount() {
        return CODE_TO_AXIS.size;
    }

    @Override
    public int getPovCount() {
        // unsupported
        return 0;
    }

    @Override
    public boolean isConnected() {
        return connected && controllerIndex.isConnected();
    }
}
