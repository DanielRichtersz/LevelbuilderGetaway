/*
 * No license.
 */
package getaway_levelbuilder;

import GameObjects.Container;
import GameObjects.Decoration;
import GameObjects.Difficulty;
import GameObjects.GameObject;
import GameObjects.GameObjectType;
import static GameObjects.GameObjectType.Floor;
import GameObjects.Level;
import GameObjects.Lever;
import GameObjects.Passage;
import GameObjects.PressurePlate;
import GameObjects.Tool;
import GameObjects.ToolType;
import GameObjects.Trap;
import Serialization.SerializeException;
import Serialization.Serializer;
import Time.Time;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

/**
 *
 * @author Maikel
 */
public class MainscreenFXMLController implements Initializable
{

    //// GUI Components.    
    @FXML
    private Text levelErrorText, room1ErrorText, room2ErrorText, roomSizeText, levelSavedText;
    @FXML
    private TextField levelNameTextField, levelMinutesTextField, levelSecondsTextField, levelSizeTextbox, targetGameobjectIDRoom1, targetGameobjectIDRoom2, xPositionTextFieldRoom1, yPositionTextFieldRoom1, xPositionTextFieldRoom2, yPositionTextFieldRoom2;
    @FXML
    private ChoiceBox levelDifficultyChoiceBox, gameobjectPickerRoom1, gameobjectPickerRoom2, gameObjectSolidRoom1, gameObjectSolidRoom2, lootObjectRoom1, lootObjectRoom2, gameObjectActivatedRoom1, gameObjectActivatedRoom2, unlockToolRoom1, unlockToolRoom2;
    @FXML
    private Button createLevelButton, addGameobjectButtonRoom1, addGameobjectButtonRoom2;
    @FXML
    private ListView gameobjectListRoom1, gameobjectListRoom2;
    @FXML
    private Tab levelTab, roomsTab;

    @FXML
    private TabPane tabPane;

    //// Fields
    private Level level;
    private List<GameObject> room1GameObjects;
    private List<GameObject> room2GameObjects;

    private boolean hasWalls = false;

    private int levelSize;
    int gameObjectIDcounter1 = 0;
    int gameObjectIDcounter2 = 1000;

    //// Button event handlers.
    @FXML
    private void createLevelButtonClick(ActionEvent event)
    {
        // Check if all fields are filled in.
        if (levelNameTextField.getText().isEmpty() || levelMinutesTextField.getText().isEmpty() || levelSecondsTextField.getText().isEmpty() || levelSizeTextbox.getText().isEmpty())
        {
            levelErrorText.setText("Please, fill in all fields.");
            return;
        }

        if (!isValidNumber(levelMinutesTextField.getText(), true) && !isValidNumber(levelSecondsTextField.getText(), true))
        {
            levelErrorText.setText("Minutes and seconds must be a number between 0 and 60.");
            return;
        }
        if (!isValidNumber(levelSizeTextbox.getText(), false) || !(Integer.parseInt(levelSecondsTextField.getText()) > 0))
        {
            levelErrorText.setText("Size must be a (positive) number.");
        }

        // Create a new Level.
        Time maxTime = new Time(Integer.parseInt(levelMinutesTextField.getText()), Integer.parseInt(levelSecondsTextField.getText()));
        Difficulty difficulty = Difficulty.valueOf(levelDifficultyChoiceBox.getValue().toString());
        level = new Level(levelNameTextField.getText(), maxTime, difficulty, Integer.parseInt(levelSizeTextbox.getText()));
        levelSize = Integer.parseInt(levelSizeTextbox.getText());
        roomSizeText.setText("Room size: " + levelSize + " x " + levelSize);

        // Empty the textfields.
        levelNameTextField.setText("");
        levelMinutesTextField.setText("");
        levelSecondsTextField.setText("");
        levelDifficultyChoiceBox.setValue(Difficulty.Easy);
        levelSizeTextbox.setText("");

        // Enable and move to the next section of the application. Disable the previous section.
        roomsTab.setDisable(false);
        tabPane.getSelectionModel().select(1);

        levelTab.setDisable(true);

        // Load all gameObjects.
        gameobjectPickerRoom1.setItems(FXCollections.observableArrayList(GameObjectType.values()));
        gameobjectPickerRoom2.setItems(FXCollections.observableArrayList(GameObjectType.values()));

        gameobjectPickerRoom1.setValue(GameObjectType.Wall);
        gameobjectPickerRoom2.setValue(GameObjectType.Wall);

        // Create outer walls.
        addOuterWalls();
    }

    @FXML
    private void addGameObjectRoom(ActionEvent event)
    {

        Button button = (Button) event.getSource();

        if (button.getId().equals("addGameobjectButtonRoom1"))
        {
            // Check if the all the fields are filled in correctly.
            if (xPositionTextFieldRoom1.getText().isEmpty() || yPositionTextFieldRoom1.getText().isEmpty())
            {
                room1ErrorText.setText("Please, fill in all fields");
                return;
            }

            if (!isValidNumber(xPositionTextFieldRoom1.getText(), false) || !isValidNumber(yPositionTextFieldRoom1.getText(), false))
            {
                room1ErrorText.setText("GameobjectID and coordinates must be a number.");
                return;
            }

            // Put the input values in a variable.
            String name = gameobjectPickerRoom1.getValue().toString();
            int gameObjectID = gameObjectIDcounter1;
            int x = Integer.parseInt(xPositionTextFieldRoom1.getText());
            int y = Integer.parseInt(yPositionTextFieldRoom1.getText());

            // Check if the coordinates are out of bounds.
            if (x < 0 || x > levelSize - 1 || y < 0 || y > levelSize - 1)
            {
                room1ErrorText.setText("Coordinates are out of bounds.");
                return;
            }

            if (!coordinatesAreFree(1, x, y))
            {
                room1ErrorText.setText("Coordinates are already used");
                return;
            }

            // Specific object checks:
            // Check if the targetID is filled in.
            if (name.equals("PressurePlate") && !isValidNumber(targetGameobjectIDRoom1.getText(), false))
            {
                room1ErrorText.setText("TargetID must be a number.");
                return;
            }

            // Create the correct gameobject and add it to the room.
            switch (name)
            {
                case "Wall":
                    Decoration wall = new Decoration(gameObjectID, GameObjectType.Wall, "", x, y, (boolean) gameObjectSolidRoom1.getSelectionModel().getSelectedItem(), true);
                    room1GameObjects.add(wall);
                    refreshList(1);
                    break;
                case "Chest":
                    Container chest = new Container(gameObjectID, GameObjectType.Chest, "", x, y, (boolean) gameObjectSolidRoom1.getSelectionModel().getSelectedItem(), true, lootObjectRoom1.getSelectionModel().getSelectedItem().getClass().getTypeName(), ToolType.valueOf(unlockToolRoom1.getSelectionModel().getSelectedItem().toString()), (boolean) gameObjectActivatedRoom1.getSelectionModel().getSelectedItem());
                    room1GameObjects.add(chest);
                    refreshList(1);
                    break;
                case "Door":
                    Passage door = new Passage(gameObjectID, GameObjectType.Door, "", x, y, (boolean) gameObjectSolidRoom1.getSelectionModel().getSelectedItem(), true);
                    room1GameObjects.add(door);
                    refreshList(1);
                    break;
                case "Lever":
                    Lever lever = new Lever(gameObjectID, GameObjectType.Lever, "", x, y, (boolean) gameObjectSolidRoom1.getSelectionModel().getSelectedItem(), true, gameObjectID, (boolean) gameObjectActivatedRoom1.getSelectionModel().getSelectedItem());
                    room1GameObjects.add(lever);
                    refreshList(1);
                    break;
                case "PressurePlate":
                    PressurePlate pressurePlate = new PressurePlate(gameObjectID, GameObjectType.PressurePlate, "", x, y, (boolean) gameObjectSolidRoom1.getSelectionModel().getSelectedItem(), true, Integer.parseInt(targetGameobjectIDRoom1.getText()), true);
                    room1GameObjects.add(pressurePlate);
                    refreshList(1);
                    break;

                case "Spikes":
                    Trap spikes = new Trap(gameObjectID, GameObjectType.Spikes, "", x, y, (boolean) gameObjectSolidRoom1.getSelectionModel().getSelectedItem(), true);
                    room1GameObjects.add(spikes);
                    refreshList(1);
                    break;

                case "Tool":
                    Tool crowbar = new Tool(gameObjectID, "", x, y, (boolean) gameObjectSolidRoom1.getSelectionModel().getSelectedItem(), true, false, ToolType.Crowbar);
                    room1GameObjects.add(crowbar);
                    refreshList(1);
                    break;
            }

            // Empty all input fields.
            gameObjectIDcounter1 = gameObjectIDcounter1 + 1;
            xPositionTextFieldRoom1.setText("");
            yPositionTextFieldRoom1.setText("");
            targetGameobjectIDRoom1.setText("");
            room1ErrorText.setText("");
        } else
        {
            // Check if the all the fields are filled in correctly.
            if (xPositionTextFieldRoom2.getText().isEmpty() || yPositionTextFieldRoom2.getText().isEmpty())
            {
                room2ErrorText.setText("Please, fill in all fields");
                return;
            }

            if (!isValidNumber(xPositionTextFieldRoom2.getText(), false) || !isValidNumber(yPositionTextFieldRoom2.getText(), false))
            {
                room2ErrorText.setText("GameobjectID and coordinates must be a number.");
                return;
            }

            // Put the input values in a variable.
            String name = gameobjectPickerRoom2.getValue().toString();
            int gameObjectID = gameObjectIDcounter2;
            int x = Integer.parseInt(xPositionTextFieldRoom2.getText());
            int y = Integer.parseInt(yPositionTextFieldRoom2.getText());

            // Check if the coordinates are out of bounds.
            if (x < 0 || x > levelSize - 1 || y < 0 || y > levelSize - 1)
            {
                room2ErrorText.setText("Coordinates are out of bounds.");
                return;
            }

            if (!coordinatesAreFree(2, x, y))
            {
                room2ErrorText.setText("Coordinates are already used");
                return;
            }

            // Specific object checks:
            // Check if the targetID is filled in.
            if (name.equals("PressurePlate") && !isValidNumber(targetGameobjectIDRoom2.getText(), false))
            {
                room2ErrorText.setText("TargetID must be a number.");
                return;
            }

            // Create the correct gameobject and add it to the room.
            switch (name)
            {
                case "Wall":
                    Decoration wall = new Decoration(gameObjectID, GameObjectType.Wall, "", x, y, (boolean) gameObjectSolidRoom2.getSelectionModel().getSelectedItem(), true);
                    room2GameObjects.add(wall);
                    refreshList(2);
                    break;
                case "Chest":
                    Container chest = new Container(gameObjectID, GameObjectType.Chest, "", x, y, (boolean) gameObjectSolidRoom2.getSelectionModel().getSelectedItem(), true, lootObjectRoom2.getSelectionModel().getSelectedItem().getClass().getTypeName(), ToolType.valueOf(unlockToolRoom2.getSelectionModel().getSelectedItem().toString()), (boolean) gameObjectActivatedRoom2.getSelectionModel().getSelectedItem());
                    room2GameObjects.add(chest);
                    refreshList(2);
                    break;
                case "Door":
                    Passage door = new Passage(gameObjectID, GameObjectType.Door, "", x, y, (boolean) gameObjectSolidRoom2.getSelectionModel().getSelectedItem(), true);
                    room2GameObjects.add(door);
                    refreshList(2);
                    break;
                case "Lever":
                    Lever lever = new Lever(gameObjectID, GameObjectType.Lever, "", x, y, (boolean) gameObjectSolidRoom2.getSelectionModel().getSelectedItem(), true, gameObjectID, (boolean) gameObjectActivatedRoom2.getSelectionModel().getSelectedItem());
                    room2GameObjects.add(lever);
                    refreshList(2);
                    break;
                case "PressurePlate":
                    PressurePlate pressurePlate = new PressurePlate(gameObjectID, GameObjectType.PressurePlate, "", x, y, (boolean) gameObjectSolidRoom2.getSelectionModel().getSelectedItem(), true, Integer.parseInt(targetGameobjectIDRoom2.getText()), true);
                    room2GameObjects.add(pressurePlate);
                    refreshList(2);
                    break;

                case "Spikes":
                    Trap spikes = new Trap(gameObjectID, GameObjectType.Spikes, "", x, y, (boolean) gameObjectSolidRoom2.getSelectionModel().getSelectedItem(), true);
                    room2GameObjects.add(spikes);
                    refreshList(2);
                    break;
                case "Tool":
                    Tool crowbar = new Tool(gameObjectID, "", x, y, (boolean) gameObjectSolidRoom2.getSelectionModel().getSelectedItem(), true, false, ToolType.Crowbar);
                    room2GameObjects.add(crowbar);
                    refreshList(2);
                    break;
            }

            // Empty all input fields.
            gameObjectIDcounter2 = gameObjectIDcounter2 + 1;
            xPositionTextFieldRoom2.setText("");
            yPositionTextFieldRoom2.setText("");
            targetGameobjectIDRoom2.setText("");
            room2ErrorText.setText("");
        }
    }

    @FXML
    private void removeGameObjectRoom(ActionEvent event)
    {
        Button button = (Button) event.getSource();

        try
        {
            if (button.getId().equals("removeGameobjectButtonRoom1"))
            {
                int index = gameobjectListRoom1.getSelectionModel().getSelectedIndex();
                room1GameObjects.remove(index);
                refreshList(1);
            } else
            {
                int index = gameobjectListRoom2.getSelectionModel().getSelectedIndex();
                room2GameObjects.remove(index);
                refreshList(2);
            }
        } catch (Exception ex)
        {
            return;
        }
    }

    /**
     * Writes the created level to a file.
     *
     * @param event Clickevent.
     */
    @FXML
    private void saveXMLFile(ActionEvent event)
    {
        int floorID = 10000;
        fillEmptyTiles();
        GameObject[][] gameObjects1 = new GameObject[levelSize][levelSize];
        GameObject[][] gameObjects2 = new GameObject[levelSize][levelSize];

        int x = 0;
        int y = 0;

        // Put the gameobjects in an array.
        for (GameObject gameObject : room1GameObjects)
        {

            x = gameObject.getPositionX();
            y = gameObject.getPositionY();

            gameObjects1[x][y] = gameObject;
        }

        for (GameObject gameObject : room2GameObjects)
        {
            x = gameObject.getPositionX();
            y = gameObject.getPositionY();

            gameObjects2[x][y] = gameObject;
        }

        // Put the gameObjects arrays into the room objects.
        level.getRoomOne().setGameObjects(gameObjects1);
        level.getRoomTwo().setGameObjects(gameObjects2);

        // Fill empty tiles room1
        for (int x1 = 0; x1 < gameObjects1.length; x1++)
        {
            for (int y1 = 0; y1 < gameObjects1[0].length; y1++)
            {
                if (gameObjects1[x1][y1] == null)
                {
                    gameObjects1[x1][y1] = new Decoration(floorID, GameObjectType.Floor, "", x1, y1, false, true);
                }
            }
        }

        // Fill empty tiles room2
        for (int x2 = 0; x2 < gameObjects2.length; x2++)
        {
            for (int y2 = 0; y2 < gameObjects2[0].length; y2++)
            {
                if (gameObjects2[x2][y2] == null)
                {
                    gameObjects2[x2][y2] = new Decoration(floorID, GameObjectType.Floor, "", x2, y2, false, true);
                }
            }
        }

        // Write the level to a file.
        Serializer serializer = new Serializer();

        try
        {
            serializer.serialize(level, level.getLevelName() + ".getawaylevel");
        } catch (SerializeException ex)
        {
            System.out.println("SerializeException: " + ex.getMessage());
        }

        levelSavedText.setVisible(true);
    }

    private void fillEmptyTiles()
    {

    }

    private void addOuterWalls()
    {
        if (!hasWalls)
        {
            // Fill room 1.
            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter1, GameObjectType.Wall, "", 0, i, true, true);
                room1GameObjects.add(wall);
                gameObjectIDcounter1++;
            }

            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter1, GameObjectType.Wall, "", i, levelSize - 1, true, true);
                room1GameObjects.add(wall);
                gameObjectIDcounter1++;
            }

            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter1, GameObjectType.Wall, "", i, 0, true, true);
                room1GameObjects.add(wall);
                gameObjectIDcounter1++;
            }

            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter1, GameObjectType.Wall, "", levelSize - 1, i, true, true);
                room1GameObjects.add(wall);
                gameObjectIDcounter1++;
            }

            // Fill room 2.
            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter2, GameObjectType.Wall, "", 0, i, true, true);
                room2GameObjects.add(wall);
                gameObjectIDcounter2++;
            }

            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter2, GameObjectType.Wall, "", i, levelSize - 1, true, true);
                room2GameObjects.add(wall);
                gameObjectIDcounter2++;
            }

            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter2, GameObjectType.Wall, "", i, 0, true, true);
                room2GameObjects.add(wall);
                gameObjectIDcounter2++;
            }

            for (int i = 0; i < levelSize; i++)
            {
                Decoration wall = new Decoration(gameObjectIDcounter2, GameObjectType.Wall, "", levelSize - 1, i, true, true);
                room2GameObjects.add(wall);
                gameObjectIDcounter2++;
            }

            // Refresh all lists.
            refreshList(1);
            refreshList(2);

            hasWalls = true;
        }

    }

    //// Other methods
    /**
     * Checks if the given value is a valid number between 0 and 60.
     *
     * @param value The value that needs to be checked.
     * @return True if the given value is a valid number.
     */
    private boolean isValidNumber(String value, boolean isTime)
    {
        try
        {
            int i = Integer.parseInt(value);

            if (isTime)
            {
                return (i >= 0 && i < 60);
            } else
            {
                return true;
            }
        } catch (NumberFormatException e)
        {
            return false;
        }
    }

    /**
     * Refreshes the list of the specified room.
     *
     * @param roomNumber Value 1 or 2: Number of the room where the list belongs
     * to.
     */
    private void refreshList(int roomNumber)
    {
        if (roomNumber == 1)
        {
            gameobjectListRoom1.setItems(FXCollections.observableArrayList(room1GameObjects));
        } else if (roomNumber == 2)
        {
            gameobjectListRoom2.setItems(FXCollections.observableArrayList(room2GameObjects));
        }
    }

    /**
     * Checks if the given coordinates are not occupied.
     *
     * @param roomNumber Number of the room that needs to be checked.
     * @param x X-coordinate of the gameObject.
     * @param y Y-coordintate of the gameObject.
     * @return True if the coordinates have no gameObject on it.
     */
    private boolean coordinatesAreFree(int roomNumber, int x, int y)
    {
        List<GameObject> gameObjectList = new ArrayList<GameObject>();

        if (roomNumber == 1)
        {
            gameObjectList = room1GameObjects;
        } else if (roomNumber == 2)
        {
            gameObjectList = room2GameObjects;
        } else
        {
            return false;
        }

        for (GameObject gameObject : gameObjectList)
        {
            if (gameObject.getPositionX() == x && gameObject.getPositionY() == y)
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        // Create all lists
        room1GameObjects = new ArrayList<>();
        room2GameObjects = new ArrayList<>();

        // Fill all choiceboxes.
        levelDifficultyChoiceBox.setItems(FXCollections.observableArrayList(Difficulty.values()));
        levelDifficultyChoiceBox.setValue(Difficulty.Easy);

        gameObjectSolidRoom1.setItems(FXCollections.observableArrayList(true, false));
        gameObjectSolidRoom1.setValue(false);

        gameObjectSolidRoom2.setItems(FXCollections.observableArrayList(true, false));
        gameObjectSolidRoom2.setValue(false);

        lootObjectRoom1.setItems(FXCollections.observableArrayList("String"));
        lootObjectRoom1.setValue("String");

        lootObjectRoom2.setItems(FXCollections.observableArrayList("String"));
        lootObjectRoom2.setValue("String");

        gameObjectActivatedRoom1.setItems(FXCollections.observableArrayList(true, false));
        gameObjectActivatedRoom1.setValue(false);
        gameObjectActivatedRoom2.setItems(FXCollections.observableArrayList(true, false));
        gameObjectActivatedRoom2.setValue(false);

        unlockToolRoom1.setItems(FXCollections.observableArrayList(ToolType.values()));
        unlockToolRoom1.setValue(ToolType.Crowbar);

        unlockToolRoom2.setItems(FXCollections.observableArrayList(ToolType.values()));
        unlockToolRoom2.setValue(ToolType.Crowbar);
    }
}
