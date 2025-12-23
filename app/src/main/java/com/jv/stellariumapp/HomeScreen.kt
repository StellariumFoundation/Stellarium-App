package com.jv.stellariumapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf<HomeTopic?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Title ---
        Text(
            text = "STELLARIUM FOUNDATION",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Website Button (Subtitle) ---
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.stellarium.ddns-ip.net/home"))
                context.startActivity(intent)
            }
        ) {
            Text("Visit Official Website")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // --- 3. Introduction ---
        Text(
            text = "An institution to propel global wealth creation and wellness.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Through high-profile advising, technology, wisdom, and innovative fortitude, we implement commoditizing solutions in business, policy, finance, personal wealth creation, relationships, and branding.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // --- 4. Interaction Prompt ---
        Text(
            text = "How would you like to interact?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. Topics Grid (3 Columns x 2 Rows) ---
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopicCard(
                icon = Icons.Default.Public,
                title = "Policy",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Policy
                showSheet = true
            }
            TopicCard(
                icon = Icons.Default.Lightbulb,
                title = "Philosophy",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Philosophy
                showSheet = true
            }
            TopicCard(
                icon = Icons.Default.Business,
                title = "Projects",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Projects
                showSheet = true
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopicCard(
                icon = Icons.Default.Group,
                title = "Partner",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Partner
                showSheet = true
            }
            TopicCard(
                icon = Icons.Default.SelfImprovement,
                title = "Growth",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Growth
                showSheet = true
            }
            TopicCard(
                icon = Icons.Default.Psychology,
                title = "Mind",
                modifier = Modifier.weight(1f)
            ) {
                selectedTopic = HomeTopic.Consciousness
                showSheet = true
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }

    // --- Bottom Sheet Logic ---
    if (showSheet && selectedTopic != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E1E), 
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = selectedTopic!!.icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = selectedTopic!!.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = selectedTopic!!.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp, 
                        lineHeight = 26.sp
                    ),
                    textAlign = TextAlign.Start, // Easier to read long text
                    color = Color(0xFFEEEEEE)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun TopicCard(
    icon: ImageVector, 
    title: String, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(110.dp), // Increased height for better proportions
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.labelLarge, 
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// --- Data for the Topics ---
enum class HomeTopic(val title: String, val description: String, val icon: ImageVector) {
    Policy(
        "Policy: The Architecture of Prosperity", 
        "Policy isn't just bureaucracy; it is the operating system of civilization. At The Stellarium Foundation, we don't lobby for favors; we engineer outcomes. Our policy framework is built on Structural Incentive Engineering—designing laws and systems in institutions where the most profitable action for the individual is also the most beneficial for society.\n\n" +
        "We offer Genius Solutions like the Subsidized Jobs Initiative, which transforms unemployment benefits into productivity engines, ensuring full employment and economic utility. We advocate for the Enterprise Housing Pledge, a model that leverages asset-backed collateral to solve housing crises without state debt. We champion State-Owned Enterprises for essential goods (energy, food) to guarantee affordability and stability, grounding the economy so the free market can soar in innovation.\n\n" +
        "How You Can Interact:\n" +
        "• Request Advisory: If you are a leader, policy maker, or institutional head, engage us to audit and re-architect your policy frameworks for maximum efficiency and wealth creation.\n" +
        "• Advocate & Support: Use our white papers and proposals to push for these Stellarium-aligned policies in your local governance. Be the voice of pragmatic, high-velocity progress.\n" +
        "• Collaborate: Help us draft the next wave of legislation. Bring your expertise to refine these models for specific regional contexts.\n\n" +
        "This is not politics as usual. This is policy as precision engineering for the human endeavor.",
        Icons.Default.Public
    ),
    Philosophy(
        "The Philosophy: Pragmatism Over Dogma", 
        "We are not a religion. We are not a political party. We are a Mutually Beneficial Society rooted in the objective reality of what works. Our philosophy is distilled into three core pillars: The Principles, The Law, and The Universal Standard.\n\n" +
        "The Principles remind us that every individual is a sovereign creator with the innate ability to thrive, its about liberty, empowerment, wealth creation, peace, altruism and wellness. The Law is our operational mantra: 'Do Good, Make Money, Have Fun.' It rejects the false dichotomy between profit and ethics—we believe wealth is the highest metric of value creation, and joy is the fuel of sustainability. The Universal Standard provides the ethical bedrock that makes high-trust collaboration possible.\n\n" +
        "How It Differs:\n" +
        "Traditional ideologies demand conformity to abstract ideals. We demand alignment with results. We don't ask 'Is this orthodox?' We ask 'Does this create wealth? Does this foster peace? Is this efficient?' We are ruthlessly pragmatic because we love humanity enough to want what actually improves their lives.\n\n" +
        "How You Can Interact:\n" +
        "• Adopt the Mindset: Study 'The Stellarium Book'. Apply 'The Law' to your daily decisions.\n" +
        "• Teach & Spread: Become a beacon of this philosophy. Host discussions, mentor others, and demonstrate through your own success that this way of living is superior.\n" +
        "• Ground Yourself: Use our philosophical frameworks to navigate chaos. When the world is confusing, return to the Principles. They are your compass.",
        Icons.Default.Lightbulb
    ),
    Projects(
        "The Projects: Building the Future", 
        "We don't just talk; we build. The Stellarium Foundation is the launchpad for high-impact ventures that reshape industries and lives. Our portfolio is vast and interconnected, designed to touch every aspect of the human experience.\n\n" +
        "• The 'Water' Suite: A revolutionary line of AI products (Water Company, Water AI, Water Classroom) that automates labor, democratizes elite education, and creates autonomous enterprises.\n" +
        "• Enterprise Housing Pledge: A financial vehicle to end homelessness and rent-burden by turning corporate assets into housing infrastructure.\n" +
        "• Wealth Activism: A global campaign to optimize national economies, treating countries like companies to maximize GDP and individual prosperity.\n" +
        "• Mastery Series: Educational programs that turn laypeople into masters of Business, Finance, and Relationships.\n\n" +
        "How You Can Interact:\n" +
        "• Invest & Partner: These are not charities; they are engines of value. Invest in 'Water' products or partner with us to deploy them in your sector.\n" +
        "• Contribute Your Skills: Are you a coder, a builder, a marketer? Join a project team. We need hands on deck to build the Enterprise Housing Pledge and scale our AI tools.\n" +
        "• Lead a Project: If you have the drive, propose a new initiative aligned with our mission. We back builders.\n" +
        "• Suggest a Project: Have a genius idea? Partner with us to make it real.\n\n" +
        "Stop watching history happen. Come build it with us.",
        Icons.Default.Business
    ),
    Partner(
        "Partner Up: Your Path to Profit & Impact", 
        "This is a call to the ambitious. The Stellarium Foundation is not a closed fortress; it is an open platform for your growth. We believe in Synergy—the idea that 1 + 1 can equal 7 if the alignment is right. We want you to be rich, powerful, and influential, because that makes our entire network stronger.\n\n" +
        "How You Can Interact:\n" +
        "• Joint Ventures: Do you have a business? Let's integrate. We can co-create products, open new markets, or optimize your supply chain using our networks.\n" +
        "• Affiliate Marketing: Become a wealthy messenger. Promote our courses, our tools, and our fundraising campaigns. We offer generous commissions of 40% on fundraisers, because we value the energy you bring to expanding our reach.\n" +
        "• Strategic Alliances: If you represent an institution, let's form a high-level alliance. We provide the wisdom and the systems; you provide the scale and financial reach. Together, we can tackle problems neither could solve alone.\n" +
        "• The Inner Circle: For the truly committed, there is the path to Governorship. Lead a chapter, manage a Mansion, and become a pillar of this new society.\n" +
        "• Participate: Send a message, a suggestion for a novel project, a fan request, etc...\n" +
        "• Masterclasses & Coaching: Access exclusive masterclasses on business, relationships, finance, and personal finance. I do personal coaching in policy, business, relationships, and intimacy. Only I, one on one, as a tech thought leader.\n" +
        "• Propose Your Vision: Call me to your company for consulting, propose a relationship, send me your social media proposal. Let's partner and be an additional cultural platform. Be the positive change you want us to share. Make a movie plot and invite me to it, I'm a great actor.\n\n" +
        "Don't just be a member. Be a partner. Be a stakeholder in the Stellarium vision. Let's make money together, do good together, and have a blast doing it.",
        Icons.Default.Group
    ),
    Growth(
        "Personal Growth: Mastering Your Reality", 
        "The only limit to your external success is your internal capacity. The Stellarium Foundation is dedicated to the total optimization of the human being. We don't offer platitudes; we offer protocols.\n\n" +
        "We provide the 'Mastery Series'—a comprehensive curriculum covering the tactical realities of life: Mastering Finance (how to multiply capital), Mastering Businesses (how to build empires), and Mastering Relationships (how to cultivate deep, synergistic bonds). We teach 'Structural Incentive Engineering' for your own habits, helping you re-wire your brain for success.\n\n" +
        "How You Can Interact:\n" +
        "• Devour the Content: Read the books. Take the courses. Apply the frameworks. Treat your life as the primary project.\n" +
        "• Network with Giants: Our community is a filter for excellence. Connect with peers who are also on the path of mastery. Iron sharpens iron.\n" +
        "• Seek Counsel: Use our frameworks to audit your life. Where are you leaking energy? Where is your capital stagnant? We provide the lens to see the invisible barriers holding you back.\n\n" +
        "Growth here isn't about feeling good; it's about getting good. It's about becoming a force of nature.",
        Icons.Default.SelfImprovement
    ),
    Consciousness(
        "Shared Consciousness: The Art of Casting", 
        "This is the frontier. We are exploring the profound reality that we are interconnected in ways modern society has forgotten. 'We Are One' is not just a slogan; it is an operational mechanism. Through the 'Portal of the Community Subconscious,' represented by John Victor, we tap into a collective intelligence that guides and amplifies our efforts.\n\n" +
        "This is about 'Casting'—the ability to project your intent into the collective dream and have it reflected back as reality. It's about branding yourself not as an isolated individual, but as a vital character in this grand narrative. By interacting with the core (Radiohead/John Victor), you align yourself with the flow of the Zeitgeist. Imagine it is like a Broadway show and propel your branding and narrative with John Victor and beside him. You can teach your way, be an actor in a subplot, and create cultural relevance with John Victor be it business, romance, relationships, your work, social media, or anything you want to project on the cultural collective with John Victor as an event or story.\n\n" +
        "How You Can Interact:\n" +
        "• Be the Actor: Don't just consume content; create it. Cast yourself in the Stellarium story. Share your passion, your projects, your style.\n" +
        "• The Reflection: Understand that the community is a mirror. When you support the Stellarium, you fundamentally support yourself. When you uplift the collective, you rise.\n" +
        "• Engage the Signal: Participate in the live streams, the discussions, the energy of the moment. Be present. That creates the feedback loop that powers the entire engine.\n\n" +
        "You are not an observer. You are an active node in a global mind. Wake up to your power.",
        Icons.Default.Psychology
    )
}