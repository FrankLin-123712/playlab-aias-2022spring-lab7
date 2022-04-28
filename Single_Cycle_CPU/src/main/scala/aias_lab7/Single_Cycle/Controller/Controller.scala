package aias_lab7.Single_Cycle.Controller

import chisel3._
import chisel3.util._


object opcode_map {
    val LOAD      = "b0000011".U
    val STORE     = "b0100011".U
    val BRANCH    = "b1100011".U
    val JALR      = "b1100111".U
    val JAL       = "b1101111".U
    val OP_IMM    = "b0010011".U
    val OP        = "b0110011".U
    val AUIPC     = "b0010111".U
    val LUI       = "b0110111".U
    val HCF       = "b0001011".U
}

object ALU_op{
  val ADD  = 0.U
  val SLL  = 1.U
  val SLT  = 2.U
  val SLTU = 3.U
  val XOR  = 4.U
  val SRL  = 5.U
  val OR   = 6.U
  val AND  = 7.U
  val SUB  = 8.U
  val SRA  = 13.U
}

object condition{
  val EQ = "b000".U
  val NE = "b001".U
  val LT = "b100".U
  val GE = "b101".U
  val LTU = "b110".U
  val GEU = "b111".U
}

import opcode_map._,condition._,ALU_op._

class Controller extends Module {
    val io = IO(new Bundle{
        val Inst = Input(UInt(32.W))
        val BrEq = Input(Bool())
        val BrLT = Input(Bool())

        val PCSel = Output(Bool())
        val ImmSel = Output(UInt(3.W))
        val RegWEn = Output(Bool())
        val BrUn = Output(Bool())
        val BSel = Output(Bool())
        val ASel = Output(Bool())
        val ALUSel = Output(UInt(4.W))
        val MemRW = Output(Bool())
        val WBSel = Output(UInt(2.W))

        //new
        val Lui = Output(Bool())
        val Hcf = Output(Bool())
    })
    
    val opcode = Wire(UInt(7.W))
    opcode := io.Inst(6,0)

    val funct3 = Wire(UInt(3.W))
    funct3 := io.Inst(14,12)

    val funct7 = Wire(UInt(1.W))
    funct7 := io.Inst(30)

    //Control signal
    io.ImmSel := MuxLookup(opcode, 0.U, Seq(
      //opcode to Immgen sel signal mapping
      // R = 0.U
      // I = 1.U
      // S = 2.U
      // B = 3.U
      // J = 4.U
      // U = 5.U
      //------------------------
      OP -> (0.U),      //R-type (OP)
      //------------------------
      OP_IMM  -> (1.U), //I-type(OP_IMM,LOAD,JALR)
      LOAD    -> (1.U),
      JALR    -> (1.U),
      //------------------------
      STORE -> (2.U),   //S-type(STORE)
      //------------------------
      BRANCH ->(3.U),   //B-type(BRANCH)
      //------------------------
      JAL -> (4.U),     //J-type(JAL)
      //------------------------
      LUI -> (5.U),     //U-type(LUI,AUIPC)
      AUIPC -> (5.U),   
      //------------------------
      HCF -> (0.U),     //HCF
    ))

    io.RegWEn := MuxLookup(opcode, 0.U, Seq(
      //1 for write, 0 for not write
      OP     -> (1.U),
      OP_IMM -> (1.U),
      LOAD   -> (1.U),
      STORE  -> (0.U),
      BRANCH -> (0.U),
      JAL    -> (1.U),
      JALR   -> (1.U),
      AUIPC  -> (1.U),
      LUI    -> (1.U),
    ))

    io.ASel   := MuxLookup(opcode, 0.U, Seq(
      //1 for pc , 0 for rdata_or_zero
      OP     -> (0.U),
      OP_IMM -> (0.U),
      LOAD   -> (0.U),
      STORE  -> (0.U),
      BRANCH -> (1.U),
      JAL    -> (1.U),
      JALR   -> (0.U),
      AUIPC  -> (1.U),
      LUI    -> (0.U),
    ))

    io.BSel   := MuxLookup(opcode, 0.U, Seq(
      //1 for imm, 0 for rdata(1)
      OP     -> (0.U),
      OP_IMM -> (1.U),
      LOAD   -> (1.U),
      STORE  -> (1.U),
      BRANCH -> (1.U),
      JAL    -> (1.U),
      JALR   -> (1.U),
      AUIPC  -> (1.U),
      LUI    -> (1.U),
    ))

    io.Lui := MuxLookup(opcode, 0.U, Seq(LUI -> (1.U))) 

    io.BrUn   := MuxLookup(opcode, 0.U, Seq(
      BRANCH -> (MuxLookup(funct3, 0.U, Seq(
        GEU -> (1.U),
        LTU -> (1.U),
      )))
    ))

    io.MemRW  := MuxLookup(opcode, 0.U, Seq(
      //1 for write, 0 for not write
      OP     -> (0.U),
      OP_IMM -> (0.U),
      LOAD   -> (0.U),
      STORE  -> (1.U),
      BRANCH -> (0.U),
      JAL    -> (0.U),
      JALR   -> (0.U),
      AUIPC  -> (0.U),
      LUI    -> (0.U),
    ))
    
    io.ALUSel := MuxLookup(opcode, 0.U, Seq(
      // Mapping opcode and funct3 to sel_signal 
      // ADD  = 0.U
      // SLL  = 1.U
      // SLT  = 2.U
      // SLTU = 3.U
      // XOR  = 4.U
      // SRL  = 5.U
      // OR   = 6.U
      // AND  = 7.U
      // SUB  = 8.U
      // SRA  = 13.U
      //--------------------------
      OP     -> (MuxLookup(funct3, 0.U, Seq(
        "b000".U -> (Mux(funct7.asBool, SUB, ADD)),
        "b001".U -> (SLL),
        "b010".U -> (SLT),
        "b011".U -> (SLTU),
        "b100".U -> (XOR),
        "b101".U -> (Mux(funct7.asBool, SRA, SRL)),
        "b110".U -> (OR),
        "b111".U -> (AND),
      ))),
      //--------------------------
      OP_IMM -> (MuxLookup(funct3, 0.U, Seq(
        "b000".U -> (ADD),
        "b001".U -> (SLL),
        "b010".U -> (SLT),
        "b011".U -> (SLTU),
        "b100".U -> (XOR),
        "b101".U -> (Mux(funct7.asBool, SRA, SRL)),
        "b110".U -> (OR),
        "b111".U -> (AND),
      ))),
      //--------------------------
      LOAD   -> (ADD),
      STORE  -> (ADD),
      BRANCH -> (ADD),
      JAL    -> (ADD),
      JALR   -> (ADD),
      AUIPC  -> (ADD),
      LUI    -> (ADD),
    ))

    
    io.PCSel := 0.U //default assignment
    switch(opcode){
      //1 for jump, 0 for no jump !
      is(BRANCH){
        switch(funct3){
          is(EQ) {io.PCSel := Mux(io.BrEq, 1.U, 0.U)}
          is(NE) {io.PCSel := Mux(!io.BrEq, 1.U, 0.U)}
          is(LT) {io.PCSel := Mux(io.BrLT, 1.U,0.U)}
          is(GE) {io.PCSel := Mux(io.BrEq||(!io.BrLT), 1.U, 0.U)}
          is(LTU){io.PCSel := Mux(io.BrLT, 1.U,0.U)}
          is(GEU){io.PCSel := Mux(io.BrEq||(!io.BrLT), 1.U, 0.U)}
        }
      }
      is(JAL,JALR){io.PCSel := 1.U}
    }
  
    io.WBSel  := MuxLookup(opcode, 0.U(2.W), Seq(
      //mapping 
      //0 -> data_mem
      //1 -> alu_out
      //2 -> pc + 4
      OP     -> (1.U(2.W)),
      OP_IMM -> (1.U(2.W)),
      LOAD   -> (0.U(2.W)),
      STORE  -> (0.U(2.W)),
      BRANCH -> (0.U(2.W)),
      JAL    -> (2.U(2.W)),
      JALR   -> (2.U(2.W)),
      AUIPC  -> (1.U(2.W)),
      LUI    -> (1.U(2.W)),
    ))
    
    io.Hcf    := MuxLookup(opcode, 0.U, Seq(
      HCF -> 1.U
    ))
}