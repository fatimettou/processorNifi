package sda.datastreaming.processor;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;
import sda.datastreaming.Travel;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({ "custom", "travel", "json", "distance", "price" })
@CapabilityDescription("TravelProcessor processes a JSON input to calculate travel distance and price, and adds the data to the output JSON.")
public class TravelProcessor extends AbstractProcessor {

    private Set<Relationship> relationships;

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Successfully processed travel data.")
            .build();

    public static final Relationship FAILURE = new Relationship.Builder()
            .name("FAILURE")
            .description("Failed to process travel data.")
            .build();

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final Set<Relationship> relationshipsSet = new HashSet<>();
        relationshipsSet.add(SUCCESS);
        relationshipsSet.add(FAILURE);
        this.relationships = Collections.unmodifiableSet(relationshipsSet);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final ComponentLog logger = getLogger();

        try {
            // Lire le contenu du flowFile
            final ByteArrayOutputStream inputContent = new ByteArrayOutputStream();
            session.exportTo(flowFile, inputContent);
            String inputJsonString = new String(inputContent.toByteArray(), StandardCharsets.UTF_8);

            String resultJsonString;
            try {
                // Appel à la méthode processJson de la classe Travel
                resultJsonString = Travel.processJson(inputJsonString);
            } catch (Exception e) {
                logger.error("Error processing travel data: " + e.getMessage(), e);
                session.transfer(flowFile, FAILURE);
                return;
            }

            // Écrire le résultat dans le FlowFile
            flowFile = session.write(flowFile, outputStream -> outputStream.write(resultJsonString.getBytes(StandardCharsets.UTF_8)));
            session.transfer(flowFile, SUCCESS);

        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage(), e);
            session.transfer(flowFile, FAILURE);
        }
    }
}
